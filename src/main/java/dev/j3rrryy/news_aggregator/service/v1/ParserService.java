package dev.j3rrryy.news_aggregator.service.v1;

import dev.j3rrryy.news_aggregator.config.ParserProperties;
import dev.j3rrryy.news_aggregator.dto.request.NewsSourceStatusRequestDto;
import dev.j3rrryy.news_aggregator.dto.response.NewsSourceStatusResponseDto;
import dev.j3rrryy.news_aggregator.enums.Source;
import dev.j3rrryy.news_aggregator.mapper.NewsSourceStatusMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ParserService {

    private final ParserProperties parserProperties;
    private final NewsSourceStatusMapper newsSourceStatusMapper;

    public NewsSourceStatusResponseDto listAll() {
        return newsSourceStatusMapper.toResponseDto(parserProperties.getSourceStatus());
    }

    public void updateAll(NewsSourceStatusRequestDto sourceStatusDto) {
        Map<Source, Boolean> currentStatus = parserProperties.getSourceStatus();
        newsSourceStatusMapper.updateStatusMap(sourceStatusDto, currentStatus);
        parserProperties.setSourceStatus(currentStatus);
    }

}
