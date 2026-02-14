@Slf4j
@Service
@RequiredArgsConstructor
public class KbSearchClientImpl implements KbSearchClient {

    private final KbSearchProperties kbSearchProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;

    private String baseUrl() {
        String url = kbSearchProperties.getOpensearch().getUrl();
        if (url == null || url.trim().isEmpty()) {
            throw new MyRuntimeException("kb.opensearch.url 未配置");
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    @Override
    public void bulkUpsert(String index, List<BulkDoc> docs) {
        if (CollectionUtils.isEmpty(docs)) return;

        try {
            // NDJSON: action line + source line + \n
            StringBuilder ndjson = new StringBuilder(docs.size() * 256);
            for (BulkDoc d : docs) {
                Map<String, Object> action = new HashMap<>();
                Map<String, Object> idx = new HashMap<>();
                idx.put("_id", d.getId());
                action.put("index", idx);

                ndjson.append(objectMapper.writeValueAsString(action)).append("\n");
                ndjson.append(objectMapper.writeValueAsString(d.getSource())).append("\n");
            }

            String url = baseUrl() + "/" + index + "/_bulk";

            HttpHeaders headers = new HttpHeaders();
            // bulk 必须是 x-ndjson
            headers.setContentType(new MediaType("application", "x-ndjson", StandardCharsets.UTF_8));

            HttpEntity<String> entity = new HttpEntity<>(ndjson.toString(), headers);

            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new MyRuntimeException("OpenSearch bulk失败: " + resp.getStatusCode());
            }

            // 检查 bulk 返回是否 errors=true
            JsonNode root = objectMapper.readTree(resp.getBody());
            boolean errors = root.path("errors").asBoolean(false);
            if (errors) {
                log.error("OpenSearch bulk errors=true, body={}", resp.getBody());
                throw new MyRuntimeException("OpenSearch bulk部分失败（errors=true），请看日志");
            }
        } catch (Exception e) {
            log.error("OpenSearch bulkUpsert error, index={}", index, e);
            throw new MyRuntimeException("OpenSearch bulk异常: " + e.getMessage());
        }
    }

    @Override
    public SearchResult searchChunks(String index, String keyword, Long kbId, Long fileId, int pageNum, int pageSize) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                throw new MyRuntimeException("keyword不能为空");
            }
            int from = Math.max(0, (Math.max(1, pageNum) - 1) * Math.max(1, pageSize));
            int size = Math.max(1, pageSize);

            Map<String, Object> bool = new LinkedHashMap<>();
            List<Object> must = new ArrayList<>();
            List<Object> filter = new ArrayList<>();

            // match content
            Map<String, Object> match = new HashMap<>();
            match.put("content", keyword);
            must.add(Collections.singletonMap("match", match));

            // 只取未删除
            filter.add(Collections.singletonMap("term", Collections.singletonMap("deletedFlag", 1)));

            if (kbId != null) {
                filter.add(Collections.singletonMap("term", Collections.singletonMap("kbId", String.valueOf(kbId))));
            }
            if (fileId != null) {
                filter.add(Collections.singletonMap("term", Collections.singletonMap("fileId", String.valueOf(fileId))));
            }

            bool.put("must", must);
            bool.put("filter", filter);

            Map<String, Object> query = Collections.singletonMap("bool", bool);

            Map<String, Object> highlight = new HashMap<>();
            // 你可以换成 <em></em> 或前端需要的标签
            highlight.put("pre_tags", Collections.singletonList("<em>"));
            highlight.put("post_tags", Collections.singletonList("</em>"));
            highlight.put("fields", Collections.singletonMap("content", Collections.singletonMap("fragment_size", 150)));

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("from", from);
            body.put("size", size);
            body.put("query", query);
            body.put("highlight", highlight);

            String url = baseUrl() + "/" + index + "/_search";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new MyRuntimeException("OpenSearch search失败: " + resp.getStatusCode());
            }

            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode hitsNode = root.path("hits");
            long total = hitsNode.path("total").path("value").asLong(0);

            List<SearchHit> hits = new ArrayList<>();
            for (JsonNode h : hitsNode.path("hits")) {
                JsonNode src = h.path("_source");
                SearchHit hit = new SearchHit();
                hit.setChunkId(src.path("chunkId").asText(null));
                hit.setKbId(src.path("kbId").asText(null));
                hit.setFileId(src.path("fileId").asText(null));
                hit.setChunkIndex(src.path("chunkIndex").isMissingNode() ? null : src.path("chunkIndex").asInt());
                hit.setScore(h.path("_score").isMissingNode() ? null : h.path("_score").asDouble());

                // highlight.content[0]
                JsonNode hl = h.path("highlight").path("content");
                if (hl.isArray() && hl.size() > 0) {
                    hit.setHighlight(hl.get(0).asText());
                } else {
                    // 没高亮就回退截断原文（可选）
                    String content = src.path("content").asText("");
                    hit.setHighlight(content.length() > 150 ? content.substring(0, 150) : content);
                }

                hits.add(hit);
            }

            SearchResult result = new SearchResult();
            result.setTotal(total);
            result.setHits(hits);
            return result;

        } catch (Exception e) {
            log.error("OpenSearch searchChunks error, index={}, keyword={}", index, keyword, e);
            throw new MyRuntimeException("OpenSearch search异常: " + e.getMessage());
        }
    }

    @Override
    public void deleteByQuery(String index, String jsonBody) {
        try {
            if (index == null || index.trim().isEmpty()) {
                throw new MyRuntimeException("index不能为空");
            }
            if (jsonBody == null || jsonBody.trim().isEmpty()) {
                throw new MyRuntimeException("deleteByQuery jsonBody不能为空");
            }

            // conflicts=proceed 避免版本冲突阻塞；refresh=true 删除后立即可见（你也可以不带，后面单独 refresh）
            String url = baseUrl() + "/" + index + "/_delete_by_query?conflicts=proceed&refresh=true";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new MyRuntimeException("OpenSearch delete_by_query失败: " + resp.getStatusCode());
            }

            // 可选：打印删了多少条，便于你验证“真正重建”
            JsonNode root = objectMapper.readTree(resp.getBody());
            long deleted = root.path("deleted").asLong(-1);
            long total = root.path("total").asLong(-1);
            log.info("OpenSearch deleteByQuery ok, index={}, total={}, deleted={}", index, total, deleted);

        } catch (Exception e) {
            log.error("OpenSearch deleteByQuery error, index={}, body={}", index, jsonBody, e);
            throw new MyRuntimeException("OpenSearch delete_by_query异常: " + e.getMessage());
        }
    }

    @Override
    public void refresh(String index) {
        try {
            if (index == null || index.trim().isEmpty()) {
                throw new MyRuntimeException("index不能为空");
            }

            String url = baseUrl() + "/" + index + "/_refresh";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>("{}", headers);

            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new MyRuntimeException("OpenSearch refresh失败: " + resp.getStatusCode());
            }

            log.info("OpenSearch refresh ok, index={}", index);
        } catch (Exception e) {
            log.error("OpenSearch refresh error, index={}", index, e);
            throw new MyRuntimeException("OpenSearch refresh异常: " + e.getMessage());
        }
    }

}
