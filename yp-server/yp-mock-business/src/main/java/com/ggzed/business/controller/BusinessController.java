package com.ggzed.business.controller;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class BusinessController {

    // 模拟查询客户风控等级
    @GetMapping("/risk")
    public Map<String, Object> checkRisk(@RequestParam String customerId) {
        System.out.println("business checkRisk customerId="+customerId);
        Map<String, Object> result = new HashMap<>();
        result.put("customerId", customerId);
        // 模拟业务逻辑：如果是 VIP 开头，就是低风险
        if (customerId.startsWith("VIP")) {
            result.put("riskLevel", "R1 (低风险)");
            result.put("message", "该客户信用良好，可购买低风险理财产品。");
        } else {
            result.put("riskLevel", "R3 (中风险)");
            result.put("message", "该客户存在一定违约风险，需谨慎推销。");
        }
        System.out.println("business checkRisk result="+result.toString());
        return result;
    }
}
