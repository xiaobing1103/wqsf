package com.wqst.api.notification;
import com.wqst.api.common.ApiResponse;
import com.wqst.api.common.PageResponse;
import com.wqst.api.common.SecuritySupport;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService service;private final SecuritySupport security;
    public NotificationController(NotificationService service,SecuritySupport security){this.service=service;this.security=security;}
    @GetMapping("/my") ApiResponse<PageResponse<NotificationEntity>> mine(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize){return ApiResponse.ok(service.mine(security.clientId(),page,pageSize));}
    @PostMapping("/{id}/read") ApiResponse<Void> read(@PathVariable long id){service.read(id,security.clientId());return ApiResponse.ok(null);}
}
