
/**
 * 知识库操作控制器类。
 *
 * @author 小青蛙
 * @date 2025-11-21
 */
@Slf4j
@RestController
@RequestMapping("/admin/app/kbLibrary")
public class KbLibraryController {

    @Autowired
    private ApplicationConfig appConfig;
    @Autowired
    private KbLibraryService kbLibraryService;

    /**
     * 新增知识库数据。
     *
     * @param kbLibraryDto 新增对象。
     * @return 应答结果对象，包含新增对象主键Id。
     */
    @SaCheckPermission("kbLibrary.add")
    @OperationLog(type = SysOperationLogType.ADD)
    @PostMapping("/add")
    public ResponseResult<Long> add(@MyRequestBody KbLibraryDto kbLibraryDto) {
        String errorMessage = MyCommonUtil.getModelValidationError(kbLibraryDto, false);
        if (errorMessage != null) {
            return ResponseResult.error(ErrorCodeEnum.DATA_VALIDATED_FAILED, errorMessage);
        }
        KbLibrary kbLibrary = MyModelUtil.copyTo(kbLibraryDto, KbLibrary.class);
        kbLibrary = kbLibraryService.saveNew(kbLibrary);
        return ResponseResult.success(kbLibrary.getId());
    }

    /**
     * 更新知识库数据。
     *
     * @param kbLibraryDto 更新对象。
     * @return 应答结果对象。
     */
    @SaCheckPermission("kbLibrary.update")
    @OperationLog(type = SysOperationLogType.UPDATE)
    @PostMapping("/update")
    public ResponseResult<Void> update(@MyRequestBody KbLibraryDto kbLibraryDto) {
        String errorMessage = MyCommonUtil.getModelValidationError(kbLibraryDto, true);
        if (errorMessage != null) {
            return ResponseResult.error(ErrorCodeEnum.DATA_VALIDATED_FAILED, errorMessage);
        }
        KbLibrary kbLibrary = MyModelUtil.copyTo(kbLibraryDto, KbLibrary.class);
        KbLibrary originalKbLibrary = kbLibraryService.getById(kbLibrary.getId());
        if (originalKbLibrary == null) {
            // NOTE: 修改下面方括号中的话述
            errorMessage = "数据验证失败，当前 [数据] 并不存在，请刷新后重试！";
            return ResponseResult.error(ErrorCodeEnum.DATA_NOT_EXIST, errorMessage);
        }

        // 自动统计字段不允许前端修改：强制保留数据库原值
        kbLibrary.setDocCount(originalKbLibrary.getDocCount());
        kbLibrary.setCallCount(originalKbLibrary.getCallCount());

        // ===================== 索引配置变更：后端维护 indexVersion/indexStatus =====================
        // 1) 规范化 indexMode（兼容旧值 BM25/EMBEDDING）
        String newMode = normalizeIndexMode(kbLibrary.getIndexMode());
        String oldMode = normalizeIndexMode(originalKbLibrary.getIndexMode());
        kbLibrary.setIndexMode(newMode);

        // 2) 判断是否发生“索引配置变更”
        // 说明：trim 之后比较，避免前端传入空格导致误判
        boolean modeChanged = !Objects.equals(
                (oldMode == null ? null : oldMode.trim()),
                (newMode == null ? null : newMode.trim())
        );
        boolean textConfigChanged = !Objects.equals(
                trimToNull(originalKbLibrary.getConfigText()),
                trimToNull(kbLibrary.getConfigText())
        );
        boolean vectorConfigChanged = !Objects.equals(
                trimToNull(originalKbLibrary.getVectorIndexConfig()),
                trimToNull(kbLibrary.getVectorIndexConfig())
        );

        // 3) 只要索引相关字段变化，就 bump 版本并标记需要重建
        if (modeChanged || textConfigChanged || vectorConfigChanged) {
            Integer oldVer = originalKbLibrary.getIndexVersion();
            kbLibrary.setIndexVersion(oldVer == null ? 1 : oldVer + 1);

            // 这里用“需要重建”语义最直观：你们如果没有 NEED_REBUILD 枚举，就用 DISABLED 或 FAILED
            // 建议：改成 "DISABLED"（你 export 里已有），并在前端用它提示“请重建索引”
            kbLibrary.setIndexStatus("DISABLED");

            log.info("[kb-index] index config changed, bump version: kbId={}, {} -> {}, status=DISABLED, changedFields={}{},{}",
                    kbLibrary.getId(),
                    oldVer, kbLibrary.getIndexVersion(),
                    modeChanged ? "mode " : "",
                    textConfigChanged ? "textConfig " : "",
                    vectorConfigChanged ? "vectorConfig" : "");
        }

        if (!kbLibraryService.update(kbLibrary, originalKbLibrary)) {
            return ResponseResult.error(ErrorCodeEnum.DATA_NOT_EXIST);
        }
        return ResponseResult.success();
    }

    /**
     * 删除知识库数据。
     *
     * @param id 删除对象主键Id。
     * @return 应答结果对象。
     */
    @SaCheckPermission("kbLibrary.delete")
    @OperationLog(type = SysOperationLogType.DELETE)
    @PostMapping("/delete")
    public ResponseResult<Void> delete(@MyRequestBody Long id) {
        if (MyCommonUtil.existBlankArgument(id)) {
            return ResponseResult.error(ErrorCodeEnum.ARGUMENT_NULL_EXIST);
        }
        return this.doDelete(id);
    }

    /**
     * 批量删除知识库数据。
     *
     * @param idList 待删除对象的主键Id列表。
     * @return 应答结果对象。
     */
    @SaCheckPermission("kbLibrary.delete")
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
     * 列出符合过滤条件的知识库列表。
     *
     * @param kbLibraryDtoFilter 过滤对象。
     * @param orderParam 排序参数。
     * @param pageParam 分页参数。
     * @return 应答结果对象，包含查询结果集。
     */
    @SaCheckPermission("kbLibrary.view")
    @PostMapping("/list")
    public ResponseResult<MyPageData<KbLibraryVo>> list(
            @MyRequestBody KbLibraryDto kbLibraryDtoFilter,
            @MyRequestBody MyOrderParam orderParam,
            @MyRequestBody MyPageParam pageParam) {
        if (pageParam != null) {
            PageMethod.startPage(pageParam.getPageNum(), pageParam.getPageSize(), pageParam.getCount());
        }
        KbLibrary kbLibraryFilter = MyModelUtil.copyTo(kbLibraryDtoFilter, KbLibrary.class);
        String orderBy = MyOrderParam.buildOrderBy(orderParam, KbLibrary.class);
        List<KbLibrary> kbLibraryList = kbLibraryService.getKbLibraryListWithRelation(kbLibraryFilter, orderBy);
        return ResponseResult.success(MyPageUtil.makeResponseData(kbLibraryList, KbLibraryVo.class));
    }

    /**
     * 导入主表数据列表。
     *
     * @param importFile 上传的文件，目前仅仅支持xlsx和xls两种格式。
     * @return 应答结果对象。
     */
    @SaCheckPermission("kbLibrary.import")
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
        List<ImportUtil.ImportHeaderInfo> headerInfoList = ImportUtil.makeHeaderInfoList(KbLibrary.class, ignoreFieldSet);
        // 下面是导入时需要注意的地方，如果我们缺省生成的代码，与实际情况存在差异，请手动修改。
        // 1. 头信息数据字段，我们只是根据当前的主表实体对象生成了缺省数组，开发者可根据实际情况，对headerInfoList进行修改。
        ImportUtil.ImportHeaderInfo[] headerInfos = headerInfoList.toArray(new ImportUtil.ImportHeaderInfo[]{});
        // 2. 这里需要根据实际情况决定，导入文件中第一行是否为中文头信息，如果是可以跳过。这里我们默认为true。
        // 这里根据自己的实际需求，为doImport的最后一个参数，传递需要进行字典转换的字段集合。
        // 注意，集合中包含需要翻译的Java字段名，如: gradeId。
        Set<String> translatedDictFieldSet = new HashSet<>();
        List<KbLibrary> dataList =
                ImportUtil.doImport(headerInfos, skipHeader, filename, KbLibrary.class, translatedDictFieldSet);
        //TODO: 下面方法的第二个字段列表参数，用于判断导入的数据在数据表是否已经存在，存在则更新，否则插入，如全部新数据，则无需任何修改。
        kbLibraryService.saveNewOrUpdateBatch(dataList, CollUtil.newArrayList(), -1);
        return ResponseResult.success();
    }

    /**
     * 导出符合过滤条件的知识库列表。
     *
     * @param kbLibraryDtoFilter 过滤对象。
     * @param orderParam 排序参数。
     * @throws IOException 文件读写失败。
     */
    @SaCheckPermission("kbLibrary.export")
    @OperationLog(type = SysOperationLogType.EXPORT, saveResponse = false)
    @PostMapping("/export")
    public void export(
            @MyRequestBody KbLibraryDto kbLibraryDtoFilter,
            @MyRequestBody MyOrderParam orderParam) throws IOException {
        KbLibrary kbLibraryFilter = MyModelUtil.copyTo(kbLibraryDtoFilter, KbLibrary.class);
        String orderBy = MyOrderParam.buildOrderBy(orderParam, KbLibrary.class);
        List<KbLibrary> resultList =
                kbLibraryService.getKbLibraryListWithRelation(kbLibraryFilter, orderBy);
        // 导出文件的标题数组
        // NOTE: 下面的代码中仅仅导出了主表数据，主表聚合计算数据和主表关联字典的数据。
        // 一对一从表数据的导出，可根据需要自行添加。如：headerMap.put("slaveFieldName.xxxField", "标题名称")
        Map<String, String> headerMap = new LinkedHashMap<>(15);
        headerMap.put("id", "主键Id");
        headerMap.put("createUserId", "创建用户Id");
        headerMap.put("createTime", "创建时间");
        headerMap.put("updateUserId", "更新用户Id");
        headerMap.put("updateTime", "更新时间");
        headerMap.put("deletedFlag", "删除标记(1: 正常 0: 已删除)");
        headerMap.put("libName", "知识库名称(用于列表卡片展示)");
        headerMap.put("kbType", "知识库类型(DOC: 文档型 FAQ: 问答型 MIXED: 混合型)");
        headerMap.put("kbDesc", "知识库描述(用于编辑页展示)");
        headerMap.put("docCount", "文档数(自动统计/卡片展示)");
        headerMap.put("callCount", "累计调用次数(自动统计/卡片展示)");
        headerMap.put("indexStatus", "索引状态(AVAILABLE: 可用 BUILDING: 构建中 FAILED: 失败 DISABLED: 禁用)");
        headerMap.put("indexMode", "索引模式(BM25: 关键词 EMBEDDING: 向量 HYBRID: 混合)");
        headerMap.put("configText", "索引配置(文本存储/不使用JSON类型)");
        // headerMap.put("configVersion", "配置版本(保存配置/重置配置)");
        ExportUtil.doExport(resultList, headerMap, "kbLibrary.xlsx");
    }

    /**
     * 保存知识库索引配置（索引版本+1，索引状态置为DISABLED）。
     *
     * @param kbId 知识库Id
     * @param indexMode 索引模式(TEXT_OS/VECTOR/HYBRID 或兼容 BM25/EMBEDDING/HYBRID)
     * @param configText 文本索引配置（可选，继续复用你现有 CONFIG_TEXT）
     * @param vectorIndexConfig 向量索引配置（可选）
     */
    @SaCheckPermission("kbLibrary.update")
    @OperationLog(type = SysOperationLogType.UPDATE)
    @PostMapping("/indexConfig/save")
    public ResponseResult<Void> saveIndexConfig(
            @MyRequestBody Long kbId,
            @MyRequestBody String indexMode,
            @MyRequestBody String configText,
            @MyRequestBody String vectorIndexConfig) {

        if (MyCommonUtil.existBlankArgument(kbId, indexMode)) {
            return ResponseResult.error(ErrorCodeEnum.ARGUMENT_NULL_EXIST);
        }

        // 校验 kb 是否存在
        KbLibrary originalKbLibrary = kbLibraryService.getById(kbId);
        if (originalKbLibrary == null) {
            String errorMessage = "数据验证失败，当前 [知识库] 并不存在，请刷新后重试！";
            return ResponseResult.error(ErrorCodeEnum.DATA_NOT_EXIST, errorMessage);
        }

        // 可选：做一下模式值白名单（避免乱传）
        // 兼容旧值：BM25/EMBEDDING/HYBRID
        String normalizedMode = normalizeIndexMode(indexMode);
        if (!isValidIndexMode(normalizedMode)) {
            return ResponseResult.error(ErrorCodeEnum.DATA_VALIDATED_FAILED,
                    "数据验证失败，索引模式不合法，仅支持 TEXT_OS/VECTOR/HYBRID（兼容 BM25/EMBEDDING/HYBRID）");
        }

        // VECTOR/HYBRID 模式下，向量配置为空就给一个最小默认（也可以留空由后端默认）
        if (("VECTOR".equals(normalizedMode) || "HYBRID".equals(normalizedMode))
                && (vectorIndexConfig == null || vectorIndexConfig.trim().isEmpty())) {
            vectorIndexConfig = "{\"embeddingModel\":\"text-embedding-v4\",\"dim\":1024,\"batch\":10,\"topK\":5}";
        }

        // 走你在 ServiceImpl 里实现的保存逻辑（会 INDEX_VERSION+1 且 INDEX_STATUS=DISABLED）
        kbLibraryService.saveIndexConfig(kbId, normalizedMode, configText, vectorIndexConfig);
        return ResponseResult.success();
    }

    /**
     * 重置知识库索引配置（索引版本+1，索引状态置为DISABLED）。
     *
     * @param kbId 知识库Id
     */
    @SaCheckPermission("kbLibrary.update")
    @OperationLog(type = SysOperationLogType.UPDATE)
    @PostMapping("/indexConfig/reset")
    public ResponseResult<Void> resetIndexConfig(@MyRequestBody Long kbId) {
        if (MyCommonUtil.existBlankArgument(kbId)) {
            return ResponseResult.error(ErrorCodeEnum.ARGUMENT_NULL_EXIST);
        }

        KbLibrary originalKbLibrary = kbLibraryService.getById(kbId);
        if (originalKbLibrary == null) {
            String errorMessage = "数据验证失败，当前 [知识库] 并不存在，请刷新后重试！";
            return ResponseResult.error(ErrorCodeEnum.DATA_NOT_EXIST, errorMessage);
        }

        kbLibraryService.resetIndexConfig(kbId);
        return ResponseResult.success();
    }

    /**
     * 查看指定知识库对象详情。
     *
     * @param id 指定对象主键Id。
     * @return 应答结果对象，包含对象详情。
     */
    @SaCheckPermission("kbLibrary.view")
    @GetMapping("/view")
    public ResponseResult<KbLibraryVo> view(@RequestParam Long id) {
        KbLibrary kbLibrary = kbLibraryService.getByIdWithRelation(id, MyRelationParam.full());
        if (kbLibrary == null) {
            return ResponseResult.error(ErrorCodeEnum.DATA_NOT_EXIST);
        }
        KbLibraryVo kbLibraryVo = MyModelUtil.copyTo(kbLibrary, KbLibraryVo.class);
        return ResponseResult.success(kbLibraryVo);
    }

    /**
     * 将历史/旧版本的索引模式值规范化为当前枚举值。
     *
     * <p>兼容旧值映射：
     * <ul>
     *   <li>BM25      -> TEXT_OS（文本检索/OpenSearch）</li>
     *   <li>EMBEDDING -> VECTOR（向量检索/Milvus 等）</li>
     *   <li>HYBRID    -> HYBRID（混合检索）</li>
     * </ul>
     *
     * <p>说明：此方法不负责“合法性校验”，只做兼容映射；未知值原样返回，交由上层校验/兜底处理。
     *
     * @param indexMode 前端/数据库传入的索引模式（可能为旧值）
     * @return 规范化后的索引模式：TEXT_OS / VECTOR / HYBRID；若入参为 null 则返回 null
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
                return indexMode; // TEXT_OS/VECTOR/HYBRID 或者本身就是 HYBRID
        }
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * 判断索引模式是否为当前系统支持的合法值。
     *
     * @param indexMode 索引模式（建议先 normalizeIndexMode 再校验）
     * @return true 表示合法（TEXT_OS / VECTOR / HYBRID）；否则 false
     */
    private boolean isValidIndexMode(String indexMode) {
        return "TEXT_OS".equals(indexMode) || "VECTOR".equals(indexMode) || "HYBRID".equals(indexMode);
    }

    /**
     * 删除知识库（软删除/逻辑删除，具体取决于 kbLibraryService.remove 的实现）。
     *
     * <p>删除前会校验目标知识库是否存在，避免对不存在数据重复删除导致前端误解。</p>
     *
     * <p>注意：此处仅删除知识库主体记录；
     * 关联文件/分段/索引清理是否级联处理，需看 remove(id) 内部是否实现，
     * 若未实现，建议在对接说明里明确“删除是否触发索引/文件级联清理”。</p>
     *
     * @param id 知识库 ID
     * @return 成功返回 ResponseResult.success()；失败返回对应错误码与提示
     */
    private ResponseResult<Void> doDelete(Long id) {
        String errorMessage;
        // 验证关联Id的数据合法性
        KbLibrary originalKbLibrary = kbLibraryService.getById(id);
        if (originalKbLibrary == null) {
            errorMessage = "数据验证失败，当前 [对象] 并不存在，请刷新后重试！";
            return ResponseResult.error(ErrorCodeEnum.DATA_NOT_EXIST, errorMessage);
        }
        if (!kbLibraryService.remove(id)) {
            errorMessage = "数据操作失败，删除的对象不存在，请刷新后重试！";
            return ResponseResult.error(ErrorCodeEnum.DATA_NOT_EXIST, errorMessage);
        }
        return ResponseResult.success();
    }
}
