@Slf4j
@Component
public class KbVectorClientImpl implements KbVectorClient {

    private final RestTemplate restTemplate;

    @Value("${kb.vector.baseUrl}")
    private String baseUrl;

    @Value("${kb.vector.reindexFilePath:/kb/vector/reindexFile}")
    private String reindexFilePath;

    public KbVectorClientImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public KbVectorReindexFileResp reindexFile(KbVectorReindexFileReq req) {
        String url = buildUrl(baseUrl, reindexFilePath);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<KbVectorReindexFileReq> entity = new HttpEntity<>(req, headers);

        try {
            ResponseEntity<KbVectorReindexFileResp> resp =
                    restTemplate.exchange(url, HttpMethod.POST, entity, KbVectorReindexFileResp.class);

            // RestTemplate 不会因为 4xx/5xx 抛异常，所以这里必须自己判
            if (!resp.getStatusCode().is2xxSuccessful()) {
                log.error("[kb-vector] call python failed, url={}, status={}, respBody={}",
                        url, resp.getStatusCodeValue(), safeTruncate(String.valueOf(resp.getBody()), 1024));
                throw new RuntimeException("python http status not 2xx: " + resp.getStatusCodeValue());
            }

            KbVectorReindexFileResp body = resp.getBody();
            if (body == null) {
                throw new RuntimeException("python resp body is null");
            }
            return body;

        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            // 万一某些场景仍然会抛（比如换了 errorHandler）
            log.error("[kb-vector] call python failed, url={}, status={}, respBody={}",
                    url, e.getStatusCode().value(), safeTruncate(e.getResponseBodyAsString(), 1024), e);
            throw e;

        } catch (Exception e) {
            log.error("[kb-vector] call python reindexFile failed, url={}", url, e);
            throw e;
        }
    }

    /** 统一拼接 baseUrl + path：避免 // 或漏 / */
    static String buildUrl(String baseUrl, String path) {
        if (baseUrl == null) baseUrl = "";
        if (path == null) path = "";

        String b = baseUrl.trim();
        String p = path.trim();

        while (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        while (p.startsWith("/")) p = p.substring(1);

        return b + "/" + p;
    }

    static String safeTruncate(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...(truncated)";
    }
}
