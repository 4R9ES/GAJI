package com.GAJI.GAJI.location;

import com.GAJI.GAJI.global.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationMapper locationMapper;

    public LocationController(LocationMapper locationMapper) {
        this.locationMapper = locationMapper;
    }

    /** 4. 동네 목록 조회 */
    @GetMapping
    public ApiResponse<List<LocationResponse>> findLocations() {
        return ApiResponse.ok("동네 목록을 조회했습니다.", locationMapper.findAll());
    }
}
