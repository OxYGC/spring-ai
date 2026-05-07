package com.gc.controller;

import com.gc.service.MysqlMemoryTripAgentService;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/agent/mysql/trip")
public class MysqlAgentController {

    private final MysqlMemoryTripAgentService mysqlMemoryTripAgentService;

    public MysqlAgentController(MysqlMemoryTripAgentService mysqlMemoryTripAgentService) {
        this.mysqlMemoryTripAgentService = mysqlMemoryTripAgentService;
    }

    /**
     * MySQL 记忆行程规划接口
     *
     * @param userId     用户唯一标识（必填，如 1001、user_999）
     * @param demand     出行需求（必填）
     * @param memoryType 记忆类型（可选，默认 message）
     */
    @GetMapping("/plan")
    public Map<String, Object> planTrip(
            @RequestParam("userId") String userId,
            @RequestParam("demand") String demand,
            @RequestParam("memoryType") String memoryType) {
        String tripPlan = mysqlMemoryTripAgentService.planTripWithMysqlMemory(userId, demand, memoryType);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "success");
        result.put("data", Map.of(
                "userId", userId,
                "memoryType", memoryType,
                "userDemand", demand,
                "tripPlan", tripPlan,
                "storageType", "MySQL 持久化"
        ));
        return result;
    }

    /**
     * 清除用户 MySQL 记忆接口（生产环境需添加权限认证）
     */
    @GetMapping("/clear-memory")
    public Map<String, Object> clearMemory(@RequestParam("userId") String userId) {
        mysqlMemoryTripAgentService.clearUserMemory(userId);
        return Map.of(
                "code", 200,
                "msg", "用户 MySQL 记忆清除成功",
                "data", Map.of("userId", userId)
        );
    }

    /**
     * 查询所有对话 ID 接口（管理端使用）
     */
    @GetMapping("/list-conversations")
    public Map<String, Object> listConversations() {
        List<String> conversationIds = mysqlMemoryTripAgentService.listAllConversationIds();
        return Map.of(
                "code", 200,
                "msg", "success",
                "data", Map.of(
                        "conversationCount", conversationIds.size(),
                        "conversationIds", conversationIds
                )
        );
    }
}
