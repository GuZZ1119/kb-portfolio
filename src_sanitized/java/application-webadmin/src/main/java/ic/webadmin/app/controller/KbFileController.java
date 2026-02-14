
/**
 * 知识库文件操作控制器类。
 *
 * @author 小青蛙
 * @date 2025-11-21
 */
@Slf4j
@RestController
@RequestMapping("/admin/app/kbFile")
public class KbFileController {

    @Autowired
    private ApplicationConfig appConfig;
    @Autowired
    private KbFileService kbFileService;
    @Autowired
    private KbJobService kbJobService;
    @Autowired
    private UpDownloaderFactory upDownloaderFactory;
    @org.springframework.beans.factory.annotation.Value("${kb.storage.localBaseDir}")
    private String kbLocalBaseDir;
    /**
     * 新增知识库文件数据。
     *
     * @param kbFileDto 新增对象。
     * @return 应答结果对象，包含新增对象主键Id.
     * 说明：
     * KbFile 记录的新增/更新主要由 /upload 与后续解析任务驱动（保证“磁盘文件”与“数据库元数据”一致）。
     * /add 与 /update 属于代码模板生成的通用 CRUD 接口，后续不作为前端对接入口使用（如需保留仅供管理员/内部调用）。
     */

    @SaCheckPermission("kbFile.add")
    @OperationLog(type = SysOperationLogType.ADD)
    @PostMapping("/add")
    public ResponseResult<Long> add(@MyRequestBody KbFileDto kbFileDto) {
        String errorMessage = MyCommonUtil.getModelValidationError(kbFileDto, false);
        if (errorMessage != null) {
            return ResponseResult.error(ErrorCodeEnum.DATA_VALIDATED_FAILED, errorMessage);
        }
        KbFile kbFile = MyModelUtil.copyTo(kbFileDto, KbFile.class);
        // 验证关联Id的数据合法性
        CallResult callResult = kbFileService.verifyRelatedData(kbFile, null);
        if (!callResult.isSuccess()) {
            return ResponseResult.errorFrom(callResult);
        }
        CallResult verifyResult = kbFileService.verifyUniqueFieldValue(kbFile, null);
        if (!verifyResult.isSuccess()) {
            return ResponseResult.errorFrom(verifyResult);
        }
        kbFile = kbFileService.saveNew(kbFile);
        return ResponseResult.success(kbFile.getId());
    }

    /**
     * 更新知识库文件数据。
     *
     * @param kbFileDto 更新对象。
     * @return 应答结果对象。
     */
    @SaCheckPermission("kbFile.update")
    @OperationLog(type = SysOperationLogType.UPDATE)
    @PostMapping("/update")
    public ResponseResult<Void> update(@MyRequestBody KbFileDto kbFileDto) {
        String errorMessage = MyCommonUtil.getModelValidationError(kbFileDto, true);
        if (errorMessage != null) {
            return ResponseResult.error(ErrorCodeEnum.DATA_VALIDATED_FAILED, errorMessage);
        }
        KbFile kbFile = MyModelUtil.copyTo(kbFileDto, KbFile.class);
        KbFile originalKbFile = kbFileService.getById(kbFile.getId());
        if (originalKbFile == null) {
            // NOTE: 修改下面方括号中的话述
            errorMessage = "数据验证失败，当前 [数据] 并不存在，请刷新后重试！";
            return ResponseResult.error(ErrorCodeEnum.DATA_NOT_EXIST, errorMessage);
        }
        CallResult verifyResult = kbFileService.verifyUniqueFieldValue(kbFile, originalKbFile);
        if (!verifyResult.isSuccess()) {
            return ResponseResult.errorFrom(verifyResult);
        }
        // 验证关联Id的数据合法性
        CallResult callResult = kbFileService.verifyRelatedData(kbFile, originalKbFile);
        if (!callResult.isSuccess()) {
            return ResponseResult.errorFrom(callResult);
        }
        if (!kbFileService.update(kbFile, originalKbFile)) {
            return ResponseResult.error(ErrorCodeEnum.DATA_NOT_EXIST);
        }
        return ResponseResult.success();
    }

    /**
     * 删除知识库文件数据。
     *
     * @param id 删除对象主键Id。
     * @return 应答结果对象。
     */
    @SaCheckPermission("kbFile.delete")
    @OperationLog(type = SysOperationLogType.DELETE)
    @PostMapping("/delete")
    public ResponseResult<Void> delete(@MyRequestBody Long id) {
        if (MyCommonUtil.existBlankArgument(id)) {
            return ResponseResult.error(ErrorCodeEnum.ARGUMENT_NULL_EXIST);
        }
        return this.doDelete(id);
    }

    /**
     * 批量删除知识库文件数据。
     *
     * @param idList 待删除对象的主键Id列表。
     * @return 应答结果对象。
     */
    @SaCheckPermission("kbFile.delete")
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
     * 列出符合过滤条件的知识库文件列表。
     *
     * @param kbFileDtoFilter 过滤对象。
     * @param orderParam 排序参数。
     * @param pageParam 分页参数。
     * @return 应答结果对象，包含查询结果集。
     */
    @SaCheckPermission("kbFile.view")
    @PostMapping("/list")
    public ResponseResult<MyPageData<KbFileVo>> list(
            @MyRequestBody KbFileDto kbFileDtoFilter,
            @MyRequestBody MyOrderParam orderParam,
            @MyRequestBody MyPageParam pageParam) {
        if (pageParam != null) {
            PageMethod.startPage(pageParam.getPageNum(), pageParam.getPageSize(), pageParam.getCount());
        }
        KbFile kbFileFilter = MyModelUtil.copyTo(kbFileDtoFilter, KbFile.class);
        String orderBy = MyOrderParam.buildOrderBy(orderParam, KbFile.class);
        List<KbFile> kbFileList = kbFileService.getKbFileListWithRelation(kbFileFilter, orderBy);
        return ResponseResult.success(MyPageUtil.makeResponseData(kbFileList, KbFileVo.class));
    }

    /**
     * 导入主表数据列表。
     *
     * @param importFile 上传的文件，目前仅仅支持xlsx和xls两种格式。
     * @return 应答结果对象。
     */
    @SaCheckPermission("kbFile.import")
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
        List<ImportUtil.ImportHeaderInfo> headerInfoList = ImportUtil.makeHeaderInfoList(KbFile.class, ignoreFieldSet);
        // 下面是导入时需要注意的地方，如果我们缺省生成的代码，与实际情况存在差异，请手动修改。
        // 1. 头信息数据字段，我们只是根据当前的主表实体对象生成了缺省数组，开发者可根据实际情况，对headerInfoList进行修改。
        ImportUtil.ImportHeaderInfo[] headerInfos = headerInfoList.toArray(new ImportUtil.ImportHeaderInfo[]{});
        // 2. 这里需要根据实际情况决定，导入文件中第一行是否为中文头信息，如果是可以跳过。这里我们默认为true。
        // 这里根据自己的实际需求，为doImport的最后一个参数，传递需要进行字典转换的字段集合。
        // 注意，集合中包含需要翻译的Java字段名，如: gradeId。
        Set<String> translatedDictFieldSet = new HashSet<>();
        translatedDictFieldSet.add("id");
        List<KbFile> dataList =
                ImportUtil.doImport(headerInfos, skipHeader, filename, KbFile.class, translatedDictFieldSet);
        CallResult result = kbFileService.verifyImportList(dataList, translatedDictFieldSet);
        if (!result.isSuccess()) {
            // result中返回了具体的验证失败对象，如果需要返回更加详细的错误，可根据实际情况手动修改。
            return ResponseResult.errorFrom(result);
        }
        //TODO: 下面方法的第二个字段列表参数，用于判断导入的数据在数据表是否已经存在，存在则更新，否则插入，如全部新数据，则无需任何修改。
        kbFileService.saveNewOrUpdateBatch(dataList, CollUtil.newArrayList(), -1);
        return ResponseResult.success();
    }

    /**
     * 导出符合过滤条件的知识库文件列表。
     *
     * @param kbFileDtoFilter 过滤对象。
     * @param orderParam 排序参数。
     * @throws IOException 文件读写失败。
     */
    @SaCheckPermission("kbFile.export")
    @OperationLog(type = SysOperationLogType.EXPORT, saveResponse = false)
    @PostMapping("/export")
    public void export(
            @MyRequestBody KbFileDto kbFileDtoFilter,
            @MyRequestBody MyOrderParam orderParam) throws IOException {
        KbFile kbFileFilter = MyModelUtil.copyTo(kbFileDtoFilter, KbFile.class);
        String orderBy = MyOrderParam.buildOrderBy(orderParam, KbFile.class);
        List<KbFile> resultList =
                kbFileService.getKbFileListWithRelation(kbFileFilter, orderBy);
        // 导出文件的标题数组
        // NOTE: 下面的代码中仅仅导出了主表数据，主表聚合计算数据和主表关联字典的数据。
        // 一对一从表数据的导出，可根据需要自行添加。如：headerMap.put("slaveFieldName.xxxField", "标题名称")
        Map<String, String> headerMap = new LinkedHashMap<>(17);
        headerMap.put("idDictMap.name", "主键Id");
        headerMap.put("createUserId", "创建用户Id");
        headerMap.put("createTime", "创建时间");
        headerMap.put("updateUserId", "更新用户Id");
        headerMap.put("updateTime", "更新时间");
        headerMap.put("deletedFlag", "删除标记(1: 正常 0: 已删除)");
        headerMap.put("kbId", "知识库Id(关联ic_kb_library.id)");
        headerMap.put("fileName", "文件名(列表展示)");
        headerMap.put("fileSize", "文件大小(字节/列表展示)");
        headerMap.put("fileExt", "文件扩展名(txt/pdf/doc/md等)");
        headerMap.put("mimeType", "文件类型(MIME)");
        headerMap.put("storageType", "存储类型(LOCAL: 本地 MINIO: 对象存储)");
        headerMap.put("storagePath", "存储路径(用于预览/下载)");
        headerMap.put("parseStatus", "解析状态(PENDING: 待解析 PARSING: 解析中 SUCCESS: 成功 FAILED: 失败)");
        headerMap.put("parseProgress", "解析进度(0-100/用于进度条)");
        headerMap.put("parseMessage", "解析结果说明(失败原因/提示信息)");
        headerMap.put("parsedTime", "解析完成时间");
        ExportUtil.doExport(resultList, headerMap, "kbFile.xlsx");
    }

    /**
     * 查看指定知识库文件对象详情。
     *
     * @param id 指定对象主键Id。
     * @return 应答结果对象，包含对象详情。
     */
    @SaCheckPermission("kbFile.view")
    @GetMapping("/view")
    public ResponseResult<KbFileVo> view(@RequestParam Long id) {
        KbFile kbFile = kbFileService.getByIdWithRelation(id, MyRelationParam.full());
        if (kbFile == null) {
            return ResponseResult.error(ErrorCodeEnum.DATA_NOT_EXIST);
        }
        KbFileVo kbFileVo = MyModelUtil.copyTo(kbFile, KbFileVo.class);
        return ResponseResult.success(kbFileVo);
    }

    private ResponseResult<Void> doDelete(Long id) {
        String errorMessage;
        // 验证关联Id的数据合法性
        KbFile originalKbFile = kbFileService.getById(id);
        if (originalKbFile == null) {
            // NOTE: 修改下面方括号中的话述
            errorMessage = "数据验证失败，当前 [对象] 并不存在，请刷新后重试！";
            return ResponseResult.error(ErrorCodeEnum.DATA_NOT_EXIST, errorMessage);
        }
        if (!kbFileService.remove(id)) {
            errorMessage = "数据操作失败，删除的对象不存在，请刷新后重试！";
            return ResponseResult.error(ErrorCodeEnum.DATA_NOT_EXIST, errorMessage);
        }
        return ResponseResult.success();
    }

    /**
     * KB 文件上传（落库 ic_kb_file）。
     */
    @SaCheckPermission("kbFile.add")
    @OperationLog(type = SysOperationLogType.UPLOAD, saveResponse = false)
    @PostMapping("/upload")
    public ResponseResult<Long> upload(
            @RequestParam Long kbId,
            @RequestParam("uploadFile") MultipartFile uploadFile) throws IOException {

        if (MyCommonUtil.existBlankArgument(kbId) || uploadFile == null || uploadFile.isEmpty()) {
            return ResponseResult.error(ErrorCodeEnum.ARGUMENT_NULL_EXIST);
        }

        String originalName = uploadFile.getOriginalFilename();
        String ext = org.apache.commons.io.FilenameUtils.getExtension(originalName);
        ext = ext == null ? null : ext.toLowerCase(Locale.ROOT);

        Set<String> allow = new HashSet<>(Arrays.asList("txt", "pdf", "doc", "docx", "md"));
        if (!allow.contains(ext)) {
            return ResponseResult.error(ErrorCodeEnum.DATA_VALIDATED_FAILED, "不支持的文件类型: " + ext);
        }

        // 关键：uriPath 只按 kbId 分目录（落地到 kbLocalBaseDir/kbId/xxx）
        String uriPath = "/" + kbId;

        BaseUpDownloader upDownloader = upDownloaderFactory.get(UploadStoreTypeEnum.LOCAL_SYSTEM);

        UploadResponseInfo info = upDownloader.doUpload(
                null,
                kbLocalBaseDir,   // ✅ 关键：用 KB 专用根目录，不要再用 appConfig.getUploadFileBaseDir()
                uriPath,
                uploadFile
        );

        if (Boolean.TRUE.equals(info.getUploadFailed())) {
            return ResponseResult.error(ErrorCodeEnum.UPLOAD_FAILED, info.getErrorMessage());
        }

        // DB 里只存相对路径：kbId/filename
        String storagePath = kbId + "/" + info.getFilename();

        KbFile kbFile = new KbFile();
        kbFile.setKbId(kbId);
        kbFile.setFileName(originalName);
        kbFile.setFileSize(uploadFile.getSize());
        kbFile.setFileExt(ext);
        kbFile.setMimeType(uploadFile.getContentType());

        kbFile.setStorageType("LOCAL");
        kbFile.setStoragePath(storagePath);

        kbFile.setParseStatus("PENDING");
        kbFile.setParseProgress(0);
        kbFile.setParseMessage(null);
        kbFile.setParsedTime(null);

        // 1) 落库 ic_kb_file
        kbFile = kbFileService.saveNew(kbFile);

        // 2) 创建解析任务 ic_kb_job
        KbJob job = new KbJob();
        job.setKbId(kbId);
        job.setJobType("PARSE_FILE");
        job.setTargetId(kbFile.getId());
        job.setStatus("PENDING");
        job.setProgress(0);

        kbJobService.saveNew(job);
        return ResponseResult.success(kbFile.getId());
    }

    /**
     * KB 文件下载（通过 ic_kb_file.id）。
     为什么这里用“流”而不是直接复用 upDownloader.doDownload(...)？
            * 1) KB 表里目前只保存了 STORAGE_PATH（我们存的是“完整文件路径含文件名”），
            * 2) LocalUpDownloader 内部带有路径安全校验（verifyFilePathSecure），其参数组合在某些场景下容易误判导致下载失败。
            *    直接按 storagePath 读文件并输出 response，最直观、最稳定。
            * 3) 等后续如果把存储信息拆成 uriPath + filename（或新增 STORAGE_FILENAME 字段），
            *    再切回 upDownloader.doDownload(rootBaseDir, uriPath, filename, response) 就能完全复用框架。
     */
    @SaCheckPermission("kbFile.view")
    @OperationLog(type = SysOperationLogType.DOWNLOAD, saveResponse = false)
    @GetMapping("/download")
    public void download(@RequestParam Long id, HttpServletResponse response) {
        if (id == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            KbFile kbFile = kbFileService.getById(id);
            if (kbFile == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            String storagePath = kbFile.getStoragePath();
            if (cn.hutool.core.util.StrUtil.isBlank(storagePath)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            Path baseDir = Paths.get(kbLocalBaseDir).toAbsolutePath().normalize();
            Path absPath = baseDir.resolve(storagePath.replace("\\", "/")).normalize();

            // 防路径穿越
            if (!absPath.startsWith(baseDir)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            File file = absPath.toFile();
            if (!file.exists()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            response.setHeader("content-type", "application/octet-stream");
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;filename=" + kbFile.getFileName());

            byte[] buff = new byte[2048];
            try (OutputStream os = response.getOutputStream();
                 BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
                int i = bis.read(buff);
                while (i != -1) {
                    os.write(buff, 0, i);
                    os.flush();
                    i = bis.read(buff);
                }
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            log.error(e.getMessage(), e);
        }
    }


}
