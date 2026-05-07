package com.gc.controller;

import com.gc.service.TripPlanningAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/agent/trip")
@RequiredArgsConstructor
public class AgentController {

    @Autowired
    private TripPlanningAgentService tripPlanningAgentService;

    /**
     * 行程规划接口：接收出行需求，返回完整行程方案
     */
    @GetMapping("/plan")
    public Map<String, String> planTrip(@RequestParam("demand") String demand) {
        String tripPlan = tripPlanningAgentService.planTrip(demand);
        return Map.of(
                "userDemand", demand,
                "tripPlan", tripPlan,
                "agentType", "智能行程规划 Agent"
        );
    }
}