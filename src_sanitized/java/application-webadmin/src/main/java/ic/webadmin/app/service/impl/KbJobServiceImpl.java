/**
 * 知识库文件解析进度数据操作服务类。
 *
 * @author 小青蛙
 * @date 2025-11-21
 */
@Slf4j
@Service("kbJobService")
public class KbJobServiceImpl extends BaseService<KbJob, Long> implements KbJobService {

    @Autowired
    private KbJobMapper kbJobMapper;

    /**
     * 返回当前Service的主表Mapper对象。
     *
     * @return 主表Mapper对象。
     */
    @Override
    protected BaseDaoMapper<KbJob> mapper() {
        return kbJobMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public KbJob saveNew(KbJob kbJob) {
        kbJobMapper.insert(this.buildDefaultValue(kbJob));
        return kbJob;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveNewBatch(List<KbJob> kbJobList) {
        if (CollUtil.isNotEmpty(kbJobList)) {
            kbJobList.forEach(this::buildDefaultValue);
            kbJobMapper.insertList(kbJobList);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveNewOrUpdateBatch(List<KbJob> kbJobList, List<String> duplicateVerifyColumns, int batchSize) {
        if (CollUtil.isEmpty(kbJobList)) {
            return;
        }
        if (batchSize <= 0) {
            batchSize = CollUtil.isNotEmpty(duplicateVerifyColumns) ? 100 : 10000;
        }
        int start = 0;
        do {
            int end = Math.min(kbJobList.size(), start + batchSize);
            List<KbJob> subList = kbJobList.subList(start, end);
            if (CollUtil.isNotEmpty(duplicateVerifyColumns)) {
                Tuple2<List<KbJob>, List<KbJob>> t = this.deduceInsertOrUpdateList(subList, duplicateVerifyColumns);
                if (CollUtil.isNotEmpty(t.getFirst())) {
                    t.getFirst().forEach(this::buildDefaultValue);
                    kbJobMapper.insertList(t.getFirst());
                }
                t.getSecond().forEach(data -> kbJobMapper.updateById(data));
            } else {
                kbJobList.forEach(this::buildDefaultValue);
                kbJobMapper.insertList(subList);
            }
            if (end == kbJobList.size()) {
                break;
            }
            start += batchSize;
        } while (true);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean update(KbJob kbJob, KbJob originalKbJob) {
        MyModelUtil.fillCommonsForUpdate(kbJob, originalKbJob);
        // 这里重点提示，在执行主表数据更新之前，如果有哪些字段不支持修改操作，请用原有数据对象字段替换当前数据字段。
        UpdateWrapper<KbJob> uw = this.createUpdateQueryForNullValue(kbJob, kbJob.getId());
        return kbJobMapper.update(kbJob, uw) == 1;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean remove(Long id) {
        return kbJobMapper.deleteById(id) == 1;
    }

    @Override
    public List<KbJob> getKbJobList(KbJob filter, String orderBy) {
        return kbJobMapper.getKbJobList(filter, orderBy);
    }

    @Override
    public List<KbJob> getKbJobListWithRelation(KbJob filter, String orderBy) {
        List<KbJob> resultList = kbJobMapper.getKbJobList(filter, orderBy);
        // 在缺省生成的代码中，如果查询结果resultList不是Page对象，说明没有分页，那么就很可能是数据导出接口调用了当前方法。
        // 为了避免一次性的大量数据关联，规避因此而造成的系统运行性能冲击，这里手动进行了分批次读取，开发者可按需修改该值。
        int batchSize = resultList instanceof Page ? 0 : 1000;
        this.buildRelationForDataList(resultList, MyRelationParam.normal(), batchSize);
        return resultList;
    }

    private KbJob buildDefaultValue(KbJob kbJob) {
        MyModelUtil.fillCommonsForInsert(kbJob);
        kbJob.setDeletedFlag(GlobalDeletedFlag.NORMAL);
        return kbJob;
    }
}
