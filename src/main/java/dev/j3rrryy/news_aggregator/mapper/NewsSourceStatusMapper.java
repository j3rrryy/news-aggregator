package dev.j3rrryy.news_aggregator.mapper;

import dev.j3rrryy.news_aggregator.dto.request.NewsSourceStatusRequestDto;
import dev.j3rrryy.news_aggregator.dto.response.NewsSourceStatusResponseDto;
import dev.j3rrryy.news_aggregator.enums.Source;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NewsSourceStatusMapper {

    public void updateStatusMap(NewsSourceStatusRequestDto requestDto, Map<Source, Boolean> statusMap) {
        if (requestDto.RT_RU() != null) statusMap.put(Source.RT_RU, requestDto.RT_RU());
        if (requestDto.AIF_RU() != null) statusMap.put(Source.AIF_RU, requestDto.AIF_RU());
        if (requestDto.SVPRESSA_RU() != null) statusMap.put(Source.SVPRESSA_RU, requestDto.SVPRESSA_RU());
    }
    
    public NewsSourceStatusResponseDto toResponseDto(Map<Source, Boolean> statusMap) {
        return new NewsSourceStatusResponseDto(
                statusMap.getOrDefault(Source.RT_RU, false),
                statusMap.getOrDefault(Source.AIF_RU, false),
                statusMap.getOrDefault(Source.SVPRESSA_RU, false)
        );
    }

}
