/**
 * 知识库文件数据操作服务类。
 *
 * @author 小青蛙
 * @date 2025-11-21
 */
@Slf4j
@Service("kbFileService")
public class KbFileServiceImpl extends BaseService<KbFile, Long> implements KbFileService {

    @Autowired
    private KbFileMapper kbFileMapper;
    @Autowired
    private KbJobService kbJobService;

    /**
     * 返回当前Service的主表Mapper对象。
     *
     * @return 主表Mapper对象。
     */
    @Override
    protected BaseDaoMapper<KbFile> mapper() {
        return kbFileMapper;
    }

    @Override
    public List<KbFile> selectByKbIdActive(Long kbId) {
        return kbFileMapper.selectByKbIdActive(kbId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public KbFile saveNew(KbFile kbFile) {
        kbFileMapper.insert(this.buildDefaultValue(kbFile));
        return kbFile;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveNewBatch(List<KbFile> kbFileList) {
        if (CollUtil.isNotEmpty(kbFileList)) {
            kbFileList.forEach(this::buildDefaultValue);
            kbFileMapper.insertList(kbFileList);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveNewOrUpdateBatch(List<KbFile> kbFileList, List<String> duplicateVerifyColumns, int batchSize) {
        if (CollUtil.isEmpty(kbFileList)) {
            return;
        }
        if (batchSize <= 0) {
            batchSize = CollUtil.isNotEmpty(duplicateVerifyColumns) ? 100 : 10000;
        }
        int start = 0;
        do {
            int end = Math.min(kbFileList.size(), start + batchSize);
            List<KbFile> subList = kbFileList.subList(start, end);
            if (CollUtil.isNotEmpty(duplicateVerifyColumns)) {
                Tuple2<List<KbFile>, List<KbFile>> t = this.deduceInsertOrUpdateList(subList, duplicateVerifyColumns);
                if (CollUtil.isNotEmpty(t.getFirst())) {
                    t.getFirst().forEach(this::buildDefaultValue);
                    kbFileMapper.insertList(t.getFirst());
                }
                t.getSecond().forEach(data -> kbFileMapper.updateById(data));
            } else {
                kbFileList.forEach(this::buildDefaultValue);
                kbFileMapper.insertList(subList);
            }
            if (end == kbFileList.size()) {
                break;
            }
            start += batchSize;
        } while (true);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean update(KbFile kbFile, KbFile originalKbFile) {
        MyModelUtil.fillCommonsForUpdate(kbFile, originalKbFile);
        // 这里重点提示，在执行主表数据更新之前，如果有哪些字段不支持修改操作，请用原有数据对象字段替换当前数据字段。
        UpdateWrapper<KbFile> uw = this.createUpdateQueryForNullValue(kbFile, kbFile.getId());
        return kbFileMapper.update(kbFile, uw) == 1;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean remove(Long id) {
        return kbFileMapper.deleteById(id) == 1;
    }

    @Override
    public List<KbFile> getKbFileList(KbFile filter, String orderBy) {
        return kbFileMapper.getKbFileList(filter, orderBy);
    }

    @Override
    public List<KbFile> getKbFileListWithRelation(KbFile filter, String orderBy) {
        List<KbFile> resultList = kbFileMapper.getKbFileList(filter, orderBy);
        // 在缺省生成的代码中，如果查询结果resultList不是Page对象，说明没有分页，那么就很可能是数据导出接口调用了当前方法。
        // 为了避免一次性的大量数据关联，规避因此而造成的系统运行性能冲击，这里手动进行了分批次读取，开发者可按需修改该值。
        int batchSize = resultList instanceof Page ? 0 : 1000;
        this.buildRelationForDataList(resultList, MyRelationParam.normal(), batchSize);
        return resultList;
    }

    @Override
    public CallResult verifyImportList(List<KbFile> dataList, Set<String> ignoreFieldSet) {
        CallResult callResult;
        if (!CollUtil.contains(ignoreFieldSet, "id")) {
            callResult = verifyImportForDatasourceDict(dataList, "idDictMap", KbFile::getId);
            if (!callResult.isSuccess()) {
                return callResult;
            }
        }
        return CallResult.ok();
    }

    @Override
    public CallResult verifyRelatedData(KbFile kbFile, KbFile originalKbFile) {
        String errorMessageFormat = "数据验证失败，关联的%s并不存在，请刷新后重试！";
        //这里是基于字典的验证。
        if (this.needToVerify(kbFile, originalKbFile, KbFile::getId)
                && !kbJobService.existId(kbFile.getId())) {
            return CallResult.error(String.format(errorMessageFormat, "主键Id"));
        }
        return CallResult.ok();
    }

    private KbFile buildDefaultValue(KbFile kbFile) {
        MyModelUtil.fillCommonsForInsert(kbFile);
        kbFile.setDeletedFlag(GlobalDeletedFlag.NORMAL);
        return kbFile;
    }
}
