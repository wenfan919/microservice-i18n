package com.yonyou.cloud.i18n.controller;

import com.yonyou.cloud.corpus.service.CorpusService;
import com.yonyou.cloud.i18n.entity.I18n;
import com.yonyou.cloud.i18n.service.I18nService;
import com.yonyou.cloud.i18n.service.I18nToolsService;
import com.yonyou.cloud.translate.entity.Translate;
import com.yonyou.cloud.translate.service.TranslateService;
import com.yonyou.i18n.constants.I18nConstants;
import com.yonyou.i18n.main.TranslateEnglish;
import com.yonyou.i18n.model.OrderedProperties;
import com.yonyou.i18n.utils.Helper;
import com.yonyou.i18n.utils.JsonFileUtil;
import com.yonyou.i18n.utils.ResourceFileUtil;
import com.yonyou.i18n.utils.ZipUtils;
import com.yonyou.iuap.baseservice.controller.GenericController;
import com.yonyou.iuap.mvc.annotation.FrontModelExchange;
import com.yonyou.iuap.mvc.type.SearchParams;
import com.yonyou.iuap.utils.PropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.*;

/**
 * 说明：国际化 基础Controller——提供数据增、删、改、查、导入导出等rest接口
 *
 * @date 2018-9-28 14:32:11
 */
@Controller
@RequestMapping(value = "/i18n")
public class I18nController extends GenericController<I18n> {

    private Logger logger = LoggerFactory.getLogger(I18nController.class);

    private I18nService i18nService;

    @Autowired
    public void settI18nService(I18nService i18nService) {
        this.i18nService = i18nService;
        super.setService(i18nService);
    }

    private I18nToolsService i18nToolsService;

    @Autowired
    public void setI18nServiceImpl(I18nToolsService i18nToolsService) {
        this.i18nToolsService = i18nToolsService;
    }

    private TranslateService translateService;

    @Autowired
    public void setTranslateService(TranslateService translateService) {
        this.translateService = translateService;
    }


    private CorpusService corpusService;

    @Autowired
    public void setCorpusService(CorpusService corpusService) {
        this.corpusService = corpusService;
    }

    @Override
    public Object list(PageRequest pageRequest,
                       @FrontModelExchange(modelType = I18n.class) SearchParams searchParams) {

        Page<I18n> page = this.i18nService.selectAllByPage(pageRequest, searchParams);

        Map<String, Object> map = new HashMap();
        map.put("data", page);
        return this.buildMapSuccess(map);

    }

    @Override
    public Object save(@RequestBody I18n entity) {
//        JsonResponse jsonResp;
//        try {
//            this.service.save(entity);
//            jsonResp = this.buildSuccess(entity);
//        } catch (Exception var4) {
//            jsonResp = this.buildError("msg", var4.getMessage(), RequestStatusEnum.FAIL_FIELD);
//        }

        return super.save(entity);
    }


    /**
     * 该部分执行下载（国际化后的文件）
     *
     * @param listData
     * @param request
     * @param response
     * @return
     */
    @RequestMapping({"/download"})
    @ResponseBody
    public Object download(@RequestBody List<I18n> listData, HttpServletRequest request, HttpServletResponse response) {

        System.out.println("++++++++++");
        return super.buildSuccess();
    }


    /**
     * 该部分执行国际化的整体逻辑
     *
     * @param listData
     * @param request
     * @param response
     * @return
     */
    @RequestMapping({"/operation"})
    @ResponseBody
    public Object operation(@RequestBody List<I18n> listData, HttpServletRequest request, HttpServletResponse response) {

        long s = System.currentTimeMillis();
        logger.info("项目工程执行开始时间：" + s);

        try {

            I18n i18n = this.i18nService.findById(listData.get(0).getId());

//            // 先赋空，然后再写入
//            i18n.setAttachId("");
//            this.i18nService.save(i18n);

            // 远程调用时传递过去的是绝对路径(即磁盘路径)，确保服务可以正常访问
            String path = PropertyUtil.getPropertyByKey("storeDir") + File.separator + i18n.getAttachment().get(0).getFileName();

            String zipPath = path.substring(0, path.lastIndexOf(".")) + "_" + System.currentTimeMillis();


            // 1: JQuery   2: React
            // projectType=JQuery
            String projectType = "React";

            if ("1".equalsIgnoreCase(i18n.getProjectType())) {
                projectType = "JQuery";

                this.i18nToolsService.operation(path, zipPath, projectType);

            } else if ("2".equalsIgnoreCase(i18n.getProjectType())) {
                projectType = "React";

                this.i18nToolsService.operation(path, zipPath, projectType);

            } else if ("3".equalsIgnoreCase(i18n.getProjectType())) {
                projectType = "Properties";

                this.i18nToolsService.operation(path, zipPath, projectType);

            } else if ("4".equalsIgnoreCase(i18n.getProjectType())) {

                // add by yy 20181123
                projectType = "English";


                /********************执行上传文件的解压缩*************************/
                logger.info("识别文件：" + path);

//                zipPath = zipPath + "/";

                logger.info("解压缩路径：" + zipPath);

                ZipUtils.unZipForFilePath(path, zipPath);


                if(!"parse".equalsIgnoreCase(i18n.getProjectStatus())){

                    OrderedProperties op = new OrderedProperties();

                    logger.info("资源解析properties！");
                    // 设置属性值
                    // resourcefileutil
                    ResourceFileUtil resourceFileUtil = new ResourceFileUtil();
                    resourceFileUtil.init(zipPath, "zh_CN.properties");

                    op.add(resourceFileUtil.getPropsFromFiles());

                    logger.info("资源解析json！");
                    // jsonfileutil
                    JsonFileUtil jsonFileUtil = new JsonFileUtil();
                    jsonFileUtil.init(zipPath, "zh_CN.json");
                    op.add(jsonFileUtil.getPropsFromFiles());


                    logger.info("资源解析完毕：" + op);

                    logger.info("开始执行资源数据库持久化操作！");

                    saveTranslate(op);

                    i18n.setProjectStatus("parse");

                    this.i18nService.save(i18n);

                    return super.buildSuccess();

                } else {


                    // 第二次执行时将翻译后的内容按文件的格式写出并生成文件
                    // 获取数据库导入的翻译的资源信息
                    logger.info("获取数据表翻译的资源，生成资源对象properties！");
                    List<Translate> transList = this.translateService.findAll();
                    Properties properties = new Properties();

                    for(Translate translate : transList){
                        if(null != translate.getEnglish() && !"".equals(translate.getEnglish())){
                            properties.put(translate.getPropertyCode(), translate.getEnglish());
                        }
                    }

                    // 读取该目录下的zh_CN.properties中文资源文件，然后将对应的key的value值变更为英文，然后保存至en_US.properties文件即可。
                    // 然后压缩并提供下载。
                    logger.info("资源获取并写入properties！");
                    // 设置属性值
                    ResourceFileUtil resourceFileUtil = new ResourceFileUtil();
                    resourceFileUtil.init(zipPath, "zh_CN.properties");

                    resourceFileUtil.setEnglishProps(properties);

                    logger.info("资源获取并写入json！");
                    JsonFileUtil jsonFileUtil = new JsonFileUtil();
                    jsonFileUtil.init(zipPath, "zh_CN.json");

                    jsonFileUtil.setEnglishProps(properties);

                }

            }

            /********************执行最后文件的压缩操作*************************/
            zipPath = zipPath.substring(zipPath.lastIndexOf("/") + 1) + I18nConstants.FILE_ZIP_POSTFIX;

            // 保存时存放是相对的可以直接下载的路径（）
            String f = i18n.getAttachment().get(0).getAccessAddress();
            f = f.substring(0, f.lastIndexOf("/")) + File.separator + zipPath;

            logger.info("最后文件的压缩路径为：" + zipPath);

            i18n.setAttachId(f);

            this.i18nService.save(i18n);

        } catch (Exception e) {
            logger.info("资源解析error：" + e);
//            e.printStackTrace();
        }

        long e = System.currentTimeMillis();
        logger.info("项目工程执行结束时间：" + e + " , 共耗时： " + (e - s) / 1000);
        return super.buildSuccess();
    }


    /**
     * 根据原始资源信息写入翻译数据表，以供其他语种的翻译
     *
     * @param properties
     * @return
     * @throws Exception
     */
    public Boolean saveTranslate(Properties properties) throws Exception {

        logger.info("开始执行原始资源信息解析并存入数据库！保存条数为：" + properties.size());

        List<String> errorList = new ArrayList<String>();
        Translate translate;

        OrderedProperties englishCorpus = corpusService.getEnglishCorpus();
        logger.info("获取数据库中英文语料库！语料条数为：" + englishCorpus.size());

        int i = 0;
        int j = 0;
        String v;
        for (String key : properties.stringPropertyNames()) {

            translate = new Translate();
            translate.setPropertyCode(key);

            v = Helper.unwindEscapeChars(properties.getProperty(key));

            translate.setChinese(v);
            translate.setEnglish(englishCorpus.getProperty(v));

            try {
                this.translateService.save(translate);
                i++;
            } catch (Exception e) {
                errorList.add(key);
                j++;
            }
        }

        logger.info("***执行资源写入数据库完成！总条数为：" + (i + j) + "***保存条数为：" + i + "***异常条数为：" + j + "***异常数据的key为：" + errorList);

        return true;

    }

}