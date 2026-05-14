package com.example.camping.service;

import com.example.camping.dto.SpotDto;
import com.example.camping.util.InstanaTracingUtil;
import com.instana.sdk.annotation.Span;
import com.instana.sdk.support.SpanSupport;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@ApplicationScoped
public class SpotService {
    private static final Logger LOGGER = Logger.getLogger(SpotService.class.getName());
    
    private static final List<SpotDto> FALLBACK_SPOTS = List.of(
            new SpotDto("11111111-1111-1111-1111-111111111111", "陽明山國家公園", "陽明山國家公園",
                    "台北近郊的山林營地，適合短程露營與自然健行。", 1200,
                    "/images/spot/Yangmingshan_National_Park.jpg", "/images/spot/Yangmingshan_National_Park.jpg"),
            new SpotDto("22222222-2222-2222-2222-222222222222", "日月潭湖畔營地", "日月潭湖畔營地",
                    "湖景第一排，適合親友旅行與放鬆假期。", 1500,
                    "/images/spot/sunmoonlake.jpg", "/images/spot/sunmoonlake.jpg"),
            new SpotDto("33333333-3333-3333-3333-333333333333", "阿里山森林營地", "阿里山森林營地",
                    "高山森林與雲海景觀，適合喜歡清晨日出的旅人。", 1800,
                    "/images/spot/Alishan_National_Scenic_Area.jpg", "/images/spot/Alishan_National_Scenic_Area.jpg"),
            new SpotDto("44444444-4444-4444-4444-444444444444", "清境草原露營區", "清境草原露營區",
                    "開闊草原與涼爽氣候，適合家庭與團體活動。", 2000,
                    "/images/spot/Qingjing_Farm.jpg", "/images/spot/Qingjing_Farm.jpg"),
            new SpotDto("55555555-5555-5555-5555-555555555555", "白沙灣海景營地", "白沙灣海景營地",
                    "靠近海岸的輕鬆營地，適合看夕陽與海邊散步。", 1300,
                    "/images/spot/Bai-Sha_Bay.jpg", "/images/spot/Bai-Sha_Bay.jpg"),
            new SpotDto("66666666-6666-6666-6666-666666666666", "九份山城體驗", "九份山城體驗",
                    "結合山城街景與夜間景觀的特色行程。", 1400,
                    "/images/spot/Jiufen_Old_Street.jpg", "/images/spot/Jiufen_Old_Street.jpg")
    );

    @Span(value = "spot.service.getAll", type = Span.Type.INTERMEDIATE)
    public List<SpotDto> getSpots() {
        return InstanaTracingUtil.trace("SpotService.getSpots", () -> {
            InstanaTracingUtil.markStep("1.access_fallback_data", "存取預設露營地點資料");
            
            InstanaTracingUtil.addBusinessTag("data.source", "FALLBACK_SPOTS");
            InstanaTracingUtil.addBusinessTag("data.count", FALLBACK_SPOTS.size());
            
            // 追蹤每個地點的基本資訊
            for (int i = 0; i < FALLBACK_SPOTS.size(); i++) {
                SpotDto spot = FALLBACK_SPOTS.get(i);
                InstanaTracingUtil.addBusinessTag("spot." + i + ".id", spot.getSpotId());
                InstanaTracingUtil.addBusinessTag("spot." + i + ".name", spot.getSpotName());
                InstanaTracingUtil.addBusinessTag("spot." + i + ".price", spot.getPrice());
            }
            
            LOGGER.info("[INSTANA-TRACE] Returning " + FALLBACK_SPOTS.size() + " spots");
            return FALLBACK_SPOTS;
        });
    }

    @Span(value = "spot.service.findById", type = Span.Type.INTERMEDIATE)
    public Optional<SpotDto> findById(String spotId) {
        return InstanaTracingUtil.trace("SpotService.findById", () -> {
            InstanaTracingUtil.markStep("1.search_in_list", "在清單中搜尋地點");
            InstanaTracingUtil.addBusinessTag("search.spot_id", spotId);
            
            Optional<SpotDto> result = InstanaTracingUtil.trace("SpotService.streamFilter", () ->
                FALLBACK_SPOTS.stream()
                    .filter(spot -> {
                        boolean matches = spot.getSpotId().equals(spotId);
                        if (matches) {
                            InstanaTracingUtil.addBusinessTag("match.found", true);
                            InstanaTracingUtil.addBusinessTag("match.spot_name", spot.getSpotName());
                            LOGGER.info("[INSTANA-TRACE] Match found: " + spot.getSpotName());
                        }
                        return matches;
                    })
                    .findFirst()
            );
            
            InstanaTracingUtil.addBusinessTag("result.present", result.isPresent());
            if (result.isPresent()) {
                SpotDto spot = result.get();
                InstanaTracingUtil.addBusinessTag("result.spot_id", spot.getSpotId());
                InstanaTracingUtil.addBusinessTag("result.spot_name", spot.getSpotName());
                InstanaTracingUtil.addBusinessTag("result.price", spot.getPrice());
                LOGGER.info("[INSTANA-TRACE] Spot found: " + spot.getSpotName());
            } else {
                LOGGER.info("[INSTANA-TRACE] Spot not found for ID: " + spotId);
            }
            
            return result;
        });
    }
}
