package com.yonyou.cloud.i18n.service;

import com.yonyou.i18n.constants.I18nConstants;
import com.yonyou.i18n.core.ExtractChar;
import com.yonyou.i18n.main.StepBy;
import com.yonyou.i18n.main.TranslateTraditional;
import com.yonyou.i18n.utils.ZipUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * 服务层： 提供国际化工具的API调用， 同时将抽取的资源写入到数据库
 *
 * @author wenfan
 */
@Service
public class I18nToolsService implements II18nToolsService {

    private static final Logger logger = LoggerFactory.getLogger(I18nToolsService.class);

    @Autowired(required = true)
    public ITranslateToolsService iTranslateToolsService;

    /**
     * 执行工具逻辑
     *
     * @param sourcePath /iuap/i18ntools/images/***.zip
     * @return
     * @throws Exception
     */
    @Override
    public String operateTools(String sourcePath) throws Exception {

        /********************执行上传文件的解压缩*************************/
        logger.info("识别文件：" + sourcePath);

        String path = sourcePath.substring(0, sourcePath.lastIndexOf(".")) + "_" + System.currentTimeMillis();

        String zipFile = path + ".zip";

        path = path + "/";

        logger.info("解压缩路径：" + path);

        ZipUtils.unZipForFilePath(sourcePath, path);

        /*********************执行国际化工具的主体方法************************/
        StepBy sb = new StepBy();

        sb.init(path);

        sb.extract();

        sb.resource();

        sb.replace();

        /*********************执行文件的压缩供下载使用************************/
        ZipUtils.zip(new File(zipFile), path);

        logger.info("执行完成后压缩路径：" + zipFile);

        /*********************资源保存完成后添加对数据库的写入操作************************/
        try {
            iTranslateToolsService.saveTranslate(sb.getPageNodesProperties(), sb.getMlrts());
        } catch (Exception e) {
            // DO Nothing
        }

        /*********************返回并写入数据库************************/
        return zipFile;
    }

    /**
     * 添加对项目类型的支持：
     * 主要处理UUI  React、 简体转繁体 的项目
     * <p>
     * 添加对资源简体转繁体的支持
     *
     * @param sourcePath  /iuap/i18ntools/images/***.zip
     * @param projectType
     * @throws Exception
     */
    @Override
    public void operateTools(String sourcePath, String projectType) throws Exception {

        /********************采用静态对象保存所有key的前缀*************************/
        // 原则上一个运行的服务只需要执行一次
        new ExtractChar().setKeyPrefixs(iTranslateToolsService.getCode());

        try {
            if (I18nConstants.PROPERTIES_PROJECT_TYPE.equalsIgnoreCase(projectType)) {
                simp2trad(sourcePath, projectType);
            } else {
                i18ntools(sourcePath, projectType);
            }

        } catch (Exception e) {
            // 异常在该部分统一处理
            logger.error(e.getMessage());
            throw e;
        }

    }


    /**
     * 添加对项目类型的支持：
     * 主要处理UUI  React的项目
     * <p>
     * 添加对资源简体转繁体的支持
     *
     * @param sourcePath  /iuap/i18ntools/images/***.zip
     * @param projectType
     * @return
     * @throws Exception
     */
    @Override
    public void operateTools(String sourcePath, String path, String projectType) throws Exception {

        /********************采用静态对象保存所有key的前缀*************************/
        // 原则上一个运行的服务只需要执行一次
        new ExtractChar().setKeyPrefixs(iTranslateToolsService.getCode());

        /********************执行上传文件的解压缩*************************/
        logger.info("识别文件：" + sourcePath);

//        String path = sourcePath.substring(0, sourcePath.lastIndexOf(".")) + "_" + System.currentTimeMillis();

        String zipFile = path + I18nConstants.FILE_ZIP_POSTFIX;

        path = path + "/";

        logger.info("解压缩路径：" + path);

        ZipUtils.unZipForFilePath(sourcePath, path);

        try {

            logger.info("开始执行核心功能！");
            if (I18nConstants.PROPERTIES_PROJECT_TYPE.equalsIgnoreCase(projectType)) {
                simp2trad(path, projectType);
            } else {
                i18ntools(path, projectType);
            }
            logger.info("执行核心功能结束！");

        } catch (Exception e) {
            // 异常在该部分统一处理

            logger.error(e.getMessage());

            throw e;

        }

        /*********************执行文件的压缩供下载使用************************/
        ZipUtils.zip(new File(zipFile), path);

        logger.info("执行完成后压缩路径：" + zipFile);


    }


    /**
     * 与jar执行国际化的工具一致，该部分被动调用
     *
     * @param path
     * @param projectType
     * @throws Exception
     */
    private void i18ntools(String path, String projectType) throws Exception {

        /*********************执行国际化工具的主体方法************************/
        StepBy sb = new StepBy();

        sb.init(path, projectType);

        sb.extract();

        sb.resource();

        sb.replace();

        /*********************资源保存完成后添加对数据库的写入操作************************/
        /*********************20181126 delete 需要开发人员手工的添加对资源的处理： 即简体转英文的处理方式************************/

    }


    /**
     * 简体转繁体
     * <p>
     * this method translate the properties file from simplified chinese to traditional chinese.
     *
     * @param path
     * @param projectType
     * @throws Exception
     */
    private void simp2trad(String path, String projectType) throws Exception {

        /*********************执行国际化工具的主体方法************************/
        TranslateTraditional sb = new TranslateTraditional();

        sb.init(path, "properties", "properties,json");

        sb.resource();

    }
}
