/**
 * 知识库文件解析进度操作控制器类。
 *
 * @author 小青蛙
 * @date 2025-11-21
 */
@Slf4j
@RestController
@RequestMapping("/admin/app/kbJob")
public class KbJobController {

    @Autowired
    private ApplicationConfig appConfig;
    @Autowired
    private KbJobService kbJobService;

    /**
     * 新增知识库文件解析进度数据。
     *
     * @param kbJobDto 新增对象。
     * @return 应答结果对象，包含新增对象主键Id。
     */
    @SaCheckPermission("kbJob.add")
    @OperationLog(type = SysOperationLogType.ADD)
    @PostMapping("/add")
    public ResponseResult<Long> add(@MyRequestBody KbJobDto kbJobDto) {
        String errorMessage = MyCommonUtil.getModelValidationError(kbJobDto, false);
        if (errorMessage != null) {
            return ResponseResult.error(ErrorCodeEnum.DATA_VALIDATED_FAILED, errorMessage);
        }
        KbJob kbJob = MyModelUtil.copyTo(kbJobDto, KbJob.class);
        kbJob = kbJobService.saveNew(kbJob);
        return ResponseResult.success(kbJob.getId());
    }

    /**
     * 更新知识库文件解析进度数据。
     *
     * @param kbJobDto 更新对象。
     * @return 应答结果对象。
     */
    @SaCheckPermission("kbJob.update")
    @OperationLog(type = SysOperationLogType.UPDATE)
    @PostMapping("/update")
    public ResponseResult<Void> update(@MyRequestBody KbJobDto kbJobDto) {
        String errorMessage = MyCommonUtil.getModelValidationError(kbJobDto, true);
        if (errorMessage != null) {
            return ResponseResult.error(ErrorCodeEnum.DATA_VALIDATED_FAILED, errorMessage);
        }
        KbJob kbJob = MyModelUtil.copyTo(kbJobDto, KbJob.class);
        KbJob originalKbJob = kbJobService.getById(kbJob.getId());
        if (originalKbJob == null) {
            // NOTE: 修改下面方括号中的话述
            errorMessage = "数据验证失败，当前 [数据] 并不存在，请刷新后重试！";
            return ResponseResult.error(ErrorCodeEnum.DATA_NOT_EXIST, errorMessage);
        }
        if (!kbJobService.update(kbJob, originalKbJob)) {
            return ResponseResult.error(ErrorCodeEnum.DATA_NOT_EXIST);
        }
        return ResponseResult.success();
    }

    /**
     * 删除知识库文件解析进度数据。
     *
     * @param id 删除对象主键Id。
     * @return 应答结果对象。
     */
    @SaCheckPermission("kbJob.delete")
    @OperationLog(type = SysOperationLogType.DELETE)
    @PostMapping("/delete")
    public ResponseResult<Void> delete(@MyRequestBody Long id) {
        if (MyCommonUtil.existBlankArgument(id)) {
            return ResponseResult.error(ErrorCodeEnum.ARGUMENT_NULL_EXIST);
        }
        return this.doDelete(id);
    }

    /**
     * 批量删除知识库文件解析进度数据。
     *
     * @param idList 待删除对象的主键Id列表。
     * @return 应答结果对象。
     */
    @SaCheckPermission("kbJob.delete")
    @OperationLog(type = SysOperationLogType.DELETE_BATCH)
    @PostMapping("/deleteBatch")
    public ResponseResult<Void> deleteBatch(@MyRequestBody List<Long> idList) {
        if (MyCommonUtil.existBlankArgument(idList)) {
            return ResponseResult.error(ErrorCodeEnum.ARGUMENT_NULL_EXIST);
        }
        for (Long id : idList) {
            ResponseResult<Void> responseResult = this.doDelete(id);
            if (!responseResult.isSuccess()) {
                return responseResult;
            }
        }
        return ResponseResult.success();
    }

    /**
     * 列出符合过滤条件的知识库文件解析进度列表。
     *
     * @param kbJobDtoFilter 过滤对象。
     * @param orderParam 排序参数。
     * @param pageParam 分页参数。
     * @return 应答结果对象，包含查询结果集。
     */
    @SaCheckPermission("kbJob.view")
    @PostMapping("/list")
    public ResponseResult<MyPageData<KbJobVo>> list(
            @MyRequestBody KbJobDto kbJobDtoFilter,
            @MyRequestBody MyOrderParam orderParam,
            @MyRequestBody MyPageParam pageParam) {
        if (pageParam != null) {
            PageMethod.startPage(pageParam.getPageNum(), pageParam.getPageSize(), pageParam.getCount());
        }
        KbJob kbJobFilter = MyModelUtil.copyTo(kbJobDtoFilter, KbJob.class);
        String orderBy = MyOrderParam.buildOrderBy(orderParam, KbJob.class);
        List<KbJob> kbJobList = kbJobService.getKbJobListWithRelation(kbJobFilter, orderBy);
        return ResponseResult.success(MyPageUtil.makeResponseData(kbJobList, KbJobVo.class));
    }

    /**
     * 导入主表数据列表。
     *
     * @param importFile 上传的文件，目前仅仅支持xlsx和xls两种格式。
     * @return 应答结果对象。
     */
    @SaCheckPermission("kbJob.import")
    @OperationLog(type = SysOperationLogType.IMPORT)
    @PostMapping("/import")
    public ResponseResult<Void> importBatch(
            @RequestParam Boolean skipHeader,
            @RequestParam("importFile") MultipartFile importFile) throws IOException {
        String filename = ImportUtil.saveImportFile(appConfig.getUploadFileBaseDir(), null, importFile);
        // 这里可以指定需要忽略导入的字段集合。如创建时间、创建人、更新时间、更新人、主键Id和逻辑删除，
        // 以及一些存在缺省值且无需导入的字段。其中主键字段和逻辑删除字段不需要在这里设置，批量插入逻辑会自动处理的。
        Set<String> ignoreFieldSet = new HashSet<>();
        ignoreFieldSet.add("createUserId");
        ignoreFieldSet.add("createTime");
        ignoreFieldSet.add("updateUserId");
        ignoreFieldSet.add("updateTime");
        List<ImportUtil.ImportHeaderInfo> headerInfoList = ImportUtil.makeHeaderInfoList(KbJob.class, ignoreFieldSet);
        // 下面是导入时需要注意的地方，如果我们缺省生成的代码，与实际情况存在差异，请手动修改。
        // 1. 头信息数据字段，我们只是根据当前的主表实体对象生成了缺省数组，开发者可根据实际情况，对headerInfoList进行修改。
        ImportUtil.ImportHeaderInfo[] headerInfos = headerInfoList.toArray(new ImportUtil.ImportHeaderInfo[]{});
        // 2. 这里需要根据实际情况决定，导入文件中第一行是否为中文头信息，如果是可以跳过。这里我们默认为true。
        // 这里根据自己的实际需求，为doImport的最后一个参数，传递需要进行字典转换的字段集合。
        // 注意，集合中包含需要翻译的Java字段名，如: gradeId。
        Set<String> translatedDictFieldSet = new HashSet<>();
        List<KbJob> dataList =
                ImportUtil.doImport(headerInfos, skipHeader, filename, KbJob.class, translatedDictFieldSet);
        //TODO: 下面方法的第二个字段列表参数，用于判断导入的数据在数据表是否已经存在，存在则更新，否则插入，如全部新数据，则无需任何修改。
        kbJobService.saveNewOrUpdateBatch(dataList, CollUtil.newArrayList(), -1);
        return ResponseResult.success();
    }

    /**
     * 导出符合过滤条件的知识库文件解析进度列表。
     *
     * @param kbJobDtoFilter 过滤对象。
     * @param orderParam 排序参数。
     * @throws IOException 文件读写失败。
     */
    @SaCheckPermission("kbJob.export")
    @OperationLog(type = SysOperationLogType.EXPORT, saveResponse = false)
    @PostMapping("/export")
    public void export(
            @MyRequestBody KbJobDto kbJobDtoFilter,
            @MyRequestBody MyOrderParam orderParam) throws IOException {
        KbJob kbJobFilter = MyModelUtil.copyTo(kbJobDtoFilter, KbJob.class);
        String orderBy = MyOrderParam.buildOrderBy(orderParam, KbJob.class);
        List<KbJob> resultList =
                kbJobService.getKbJobListWithRelation(kbJobFilter, orderBy);
        // 导出文件的标题数组
        // NOTE: 下面的代码中仅仅导出了主表数据，主表聚合计算数据和主表关联字典的数据。
        // 一对一从表数据的导出，可根据需要自行添加。如：headerMap.put("slaveFieldName.xxxField", "标题名称")
        Map<String, String> headerMap = new LinkedHashMap<>(14);
        headerMap.put("id", "主键Id");
        headerMap.put("createUserId", "创建用户Id");
        headerMap.put("createTime", "创建时间");
        headerMap.put("updateUserId", "更新用户Id");
        headerMap.put("updateTime", "更新时间");
        headerMap.put("deletedFlag", "删除标记(1: 正常 0: 已删除)");
        headerMap.put("kbId", "知识库Id(关联ic_kb_library.id)");
        headerMap.put("jobType", "任务类型(REBUILD_INDEX: 重建索引 PARSE_FILE: 解析文件)");
        headerMap.put("targetId", "关联目标Id(如file_id/为空表示全库任务)");
        headerMap.put("status", "任务状态(PENDING: 排队 RUNNING: 运行 SUCCESS: 成功 FAILED: 失败)");
        headerMap.put("progress", "任务进度(0-100/用于进度条)");
        headerMap.put("message", "任务信息(失败原因/运行说明)");
        headerMap.put("startTime", "开始时间");
        headerMap.put("endTime", "结束时间");
        ExportUtil.doExport(resultList, headerMap, "kbJob.xlsx");
    }

    /**
     * 查看指定知识库文件解析进度对象详情。
     *
     * @param id 指定对象主键Id。
     * @return 应答结果对象，包含对象详情。
     */
    @SaCheckPermission("kbJob.view")
    @GetMapping("/view")
    public ResponseResult<KbJobVo> view(@RequestParam Long id) {
        KbJob kbJob = kbJobService.getByIdWithRelation(id, MyRelationParam.full());
        if (kbJob == null) {
            return ResponseResult.error(ErrorCodeEnum.DATA_NOT_EXIST);
        }
        KbJobVo kbJobVo = MyModelUtil.copyTo(kbJob, KbJobVo.class);
        return ResponseResult.success(kbJobVo);
    }

    /**
     * 以字典形式返回全部知识库文件解析进度数据集合。字典的键值为[id, targetId]。
     * 白名单接口，登录用户均可访问。
     *
     * @param filter 过滤对象。
     * @return 应答结果对象，包含的数据为 List<Map<String, String>>，map中包含两条记录，key的值分别是id和name，value对应具体数据。
     */
    @GetMapping("/listDict")
    public ResponseResult<List<Map<String, Object>>> listDict(KbJobDto filter) {
        List<KbJob> resultList =
                kbJobService.getListByFilter(MyModelUtil.copyTo(filter, KbJob.class));
        return ResponseResult.success(MyCommonUtil.toDictDataList(
                resultList, KbJob::getId, KbJob::getTargetId));
    }

    /**
     * 根据字典Id集合，获取查询后的字典数据。
     *
     * @param dictIds 字典Id集合。
     * @return 应答结果对象，包含字典形式的数据集合。
     */
    @GetMapping("/listDictByIds")
    public ResponseResult<List<Map<String, Object>>> listDictByIds(@RequestParam List<Long> dictIds) {
        List<KbJob> resultList = kbJobService.getInList(new HashSet<>(dictIds));
        return ResponseResult.success(MyCommonUtil.toDictDataList(
                resultList, KbJob::getId, KbJob::getTargetId));
    }

    private ResponseResult<Void> doDelete(Long id) {
        String errorMessage;
        // 验证关联Id的数据合法性
        KbJob originalKbJob = kbJobService.getById(id);
        if (originalKbJob == null) {
            // NOTE: 修改下面方括号中的话述
            errorMessage = "数据验证失败，当前 [对象] 并不存在，请刷新后重试！";
            return ResponseResult.error(ErrorCodeEnum.DATA_NOT_EXIST, errorMessage);
        }
        if (!kbJobService.remove(id)) {
            errorMessage = "数据操作失败，删除的对象不存在，请刷新后重试！";
            return ResponseResult.error(ErrorCodeEnum.DATA_NOT_EXIST, errorMessage);
        }
        return ResponseResult.success();
    }
}
