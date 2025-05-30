package dev.j3rrryy.news_aggregator.mapper;

import dev.j3rrryy.news_aggregator.dto.request.NewsSourceStatusesRequest;
import dev.j3rrryy.news_aggregator.dto.response.NewsSourceStatusesResponse;
import dev.j3rrryy.news_aggregator.enums.Source;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NewsSourceStatusesMapper {

    public void updateStatusMap(NewsSourceStatusesRequest requestDto, Map<Source, Boolean> statusMap) {
        if (requestDto.rtRu() != null) statusMap.put(Source.RT_RU, requestDto.rtRu());
        if (requestDto.aifRu() != null) statusMap.put(Source.AIF_RU, requestDto.aifRu());
        if (requestDto.svpressaRu() != null) statusMap.put(Source.SVPRESSA_RU, requestDto.svpressaRu());
    }

    public NewsSourceStatusesResponse toResponseDto(Map<Source, Boolean> statusMap) {
        return new NewsSourceStatusesResponse(
                statusMap.getOrDefault(Source.RT_RU, false),
                statusMap.getOrDefault(Source.AIF_RU, false),
                statusMap.getOrDefault(Source.SVPRESSA_RU, false)
        );
    }

}
