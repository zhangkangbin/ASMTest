package com.asmtest

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 注册插件入口
 *
 */
public class AsmPlugin implements Plugin<Project> {
    public static final String EXT_NAME = 'AsmTestPlugin'

    @Override
    public void apply(Project project) {
        /**
         * 注册transform接口
         */

        def android = project.extensions.getByType(AppExtension)
        def transformImpl = new AsmTransform()
        android.registerTransform(transformImpl)
        print("-------------------------注册transform接口6------------------------------")



    }


}
