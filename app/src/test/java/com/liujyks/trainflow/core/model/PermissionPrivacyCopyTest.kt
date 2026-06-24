package com.liujyks.trainflow.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionPrivacyCopyTest {
    @Test
    fun sectionsCoverUserTestingPermissionAndPrivacyBoundaries() {
        val copy = PermissionPrivacyCopy.sections.associate { section -> section.title to section.body }

        assertEquals(7, copy.size)
        assertTrue(copy.getValue("通知权限").contains("计划提醒"))
        assertTrue(copy.getValue("通知权限").contains("训练中状态提示"))
        assertTrue(copy.getValue("通知权限").contains("训练仍可正常使用"))
        assertTrue(copy.getValue("通知权限").contains("系统延迟"))
        assertTrue(copy.getValue("活跃训练通知").contains("状态摘要"))
        assertTrue(copy.getValue("活跃训练通知").contains("不是 foreground service"))
        assertTrue(copy.getValue("健康数据").contains("不显示心率"))
        assertTrue(copy.getValue("健康数据").contains("不录入心率"))
        assertTrue(copy.getValue("健康数据").contains("不统计心率"))
        assertTrue(copy.getValue("健康数据").contains("未接入真实设备"))
        assertTrue(copy.getValue("恢复建议").contains("基础放松映射"))
        assertTrue(copy.getValue("音频提示").contains("不降低、暂停或打断"))
        assertTrue(copy.getValue("语音").contains("未实现语音控制"))
        assertTrue(copy.getValue("数据").contains("内存态"))
    }

    @Test
    fun copyDoesNotClaimUnavailableMedicalDeviceVoiceOrStorageCapabilities() {
        val allCopy = PermissionPrivacyCopy.sections.joinToString(" ") { section ->
            "${section.title} ${section.body}"
        }
        val forbiddenClaims = listOf(
            "闹钟级强提醒已启用",
            "后台可靠计时已完成",
            "foreground service 已启用",
            "已接入真实设备",
            "支持手环连接",
            "支持手表连接",
            "Health Connect 已接入",
            "语音教练已启用",
            "语音读秒已启用",
            "医疗诊断结果",
            "危险心率",
            "康复治疗方案",
            "疼痛处理方案",
            "云同步已完成",
            "账号体系已完成",
            "已支持真实长期记录"
        )

        forbiddenClaims.forEach { claim ->
            assertFalse(allCopy.contains(claim))
        }
    }
}
