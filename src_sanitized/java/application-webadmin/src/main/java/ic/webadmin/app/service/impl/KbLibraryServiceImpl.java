/**
 * 知识库数据操作服务类。
 *
 * @author 小青蛙
 * @date 2025-11-21
 */
@Slf4j
@Service("kbLibraryService")
public class KbLibraryServiceImpl extends BaseService<KbLibrary, Long> implements KbLibraryService {

    @Autowired
    private KbLibraryMapper kbLibraryMapper;

    /**
     * 返回当前Service的主表Mapper对象。
     *
     * @return 主表Mapper对象。
     */
    @Override
    protected BaseDaoMapper<KbLibrary> mapper() {
        return kbLibraryMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public KbLibrary saveNew(KbLibrary kbLibrary) {
        kbLibraryMapper.insert(this.buildDefaultValue(kbLibrary));
        return kbLibrary;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveNewBatch(List<KbLibrary> kbLibraryList) {
        if (CollUtil.isNotEmpty(kbLibraryList)) {
            kbLibraryList.forEach(this::buildDefaultValue);
            kbLibraryMapper.insertList(kbLibraryList);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveNewOrUpdateBatch(List<KbLibrary> kbLibraryList, List<String> duplicateVerifyColumns, int batchSize) {
        if (CollUtil.isEmpty(kbLibraryList)) {
            return;
        }
        if (batchSize <= 0) {
            batchSize = CollUtil.isNotEmpty(duplicateVerifyColumns) ? 100 : 10000;
        }
        int start = 0;
        do {
            int end = Math.min(kbLibraryList.size(), start + batchSize);
            List<KbLibrary> subList = kbLibraryList.subList(start, end);
            if (CollUtil.isNotEmpty(duplicateVerifyColumns)) {
                Tuple2<List<KbLibrary>, List<KbLibrary>> t = this.deduceInsertOrUpdateList(subList, duplicateVerifyColumns);
                if (CollUtil.isNotEmpty(t.getFirst())) {
                    t.getFirst().forEach(this::buildDefaultValue);
                    kbLibraryMapper.insertList(t.getFirst());
                }
                t.getSecond().forEach(data -> kbLibraryMapper.updateById(data));
            } else {
                kbLibraryList.forEach(this::buildDefaultValue);
                kbLibraryMapper.insertList(subList);
            }
            if (end == kbLibraryList.size()) {
                break;
            }
            start += batchSize;
        } while (true);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean update(KbLibrary kbLibrary, KbLibrary originalKbLibrary) {
        MyModelUtil.fillCommonsForUpdate(kbLibrary, originalKbLibrary);
        // 这里重点提示，在执行主表数据更新之前，如果有哪些字段不支持修改操作，请用原有数据对象字段替换当前数据字段。
        UpdateWrapper<KbLibrary> uw = this.createUpdateQueryForNullValue(kbLibrary, kbLibrary.getId());
        return kbLibraryMapper.update(kbLibrary, uw) == 1;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean remove(Long id) {
        return kbLibraryMapper.deleteById(id) == 1;
    }

    @Override
    public List<KbLibrary> getKbLibraryList(KbLibrary filter, String orderBy) {
        return kbLibraryMapper.getKbLibraryList(filter, orderBy);
    }

    @Override
    public List<KbLibrary> getKbLibraryListWithRelation(KbLibrary filter, String orderBy) {
        List<KbLibrary> resultList = kbLibraryMapper.getKbLibraryList(filter, orderBy);
        // 在缺省生成的代码中，如果查询结果resultList不是Page对象，说明没有分页，那么就很可能是数据导出接口调用了当前方法。
        // 为了避免一次性的大量数据关联，规避因此而造成的系统运行性能冲击，这里手动进行了分批次读取，开发者可按需修改该值。
        int batchSize = resultList instanceof Page ? 0 : 1000;
        this.buildRelationForDataList(resultList, MyRelationParam.normal(), batchSize);
        return resultList;
    }

    private KbLibrary buildDefaultValue(KbLibrary kbLibrary) {
        MyModelUtil.fillCommonsForInsert(kbLibrary);
        kbLibrary.setDeletedFlag(GlobalDeletedFlag.NORMAL);

        // 自动统计字段：新增时默认 0
        if (kbLibrary.getDocCount() == null) {
            kbLibrary.setDocCount(0);
        }
        if (kbLibrary.getCallCount() == null) {
            kbLibrary.setCallCount(0L);
        }
        return kbLibrary;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveIndexConfig(Long kbId, String indexMode, String configText, String vectorIndexConfig) {
        KbLibrary update = new KbLibrary();
        update.setId(kbId);
        update.setIndexMode(normalizeIndexMode(indexMode));
        update.setConfigText(configText);
        update.setVectorIndexConfig(vectorIndexConfig);

        // 填充更新公共字段
        MyModelUtil.fillCommonsForUpdate(update, this.getById(kbId));

        int rows = kbLibraryMapper.updateIndexConfigById(update);
        if (rows != 1) {
            throw new RuntimeException("保存索引配置失败，kbId=" + kbId);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void resetIndexConfig(Long kbId) {
        KbLibrary update = new KbLibrary();
        update.setId(kbId);
        update.setIndexMode("TEXT_OS");          // 默认关键词
        update.setConfigText("{}");              // 文本索引默认配置
        update.setVectorIndexConfig("{}");       // 向量索引默认配置

        MyModelUtil.fillCommonsForUpdate(update, this.getById(kbId));

        int rows = kbLibraryMapper.updateIndexConfigById(update);
        if (rows != 1) {
            throw new RuntimeException("重置索引配置失败，kbId=" + kbId);
        }
    }

    /**
     * 兼容旧值：BM25/EMBEDDING/HYBRID -> TEXT_OS/VECTOR/HYBRID
     */
    private String normalizeIndexMode(String indexMode) {
        if (indexMode == null) {
            return null;
        }
        switch (indexMode) {
            case "BM25":
                return "TEXT_OS";
            case "EMBEDDING":
                return "VECTOR";
            default:
                return indexMode; // TEXT_OS/VECTOR/HYBRID 或 HYBRID
        }
    }

}
