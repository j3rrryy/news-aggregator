package dev.j3rrryy.news_aggregator.service.v1;

import dev.j3rrryy.news_aggregator.config.ParserProperties;
import dev.j3rrryy.news_aggregator.dto.request.AutoParsingIntervalDto;
import dev.j3rrryy.news_aggregator.dto.request.NewsSourceStatusRequestDto;
import dev.j3rrryy.news_aggregator.dto.response.AutoParsingStatusDto;
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

    public NewsSourceStatusResponseDto getSourceStatus() {
        return newsSourceStatusMapper.toResponseDto(parserProperties.getSourceStatus());
    }

    public void patchSourceStatus(NewsSourceStatusRequestDto newsSourceStatusRequestDto) {
        Map<Source, Boolean> currentStatus = parserProperties.getSourceStatus();
        newsSourceStatusMapper.updateStatusMap(newsSourceStatusRequestDto, currentStatus);
        parserProperties.setSourceStatus(currentStatus);
    }

    public AutoParsingStatusDto getAutoParsingStatus() {
        return new AutoParsingStatusDto(
                parserProperties.isAutoParsingEnabled(),
                parserProperties.getAutoParsingInterval()
        );
    }

    public void setAutoParsingInterval(AutoParsingIntervalDto autoParsingIntervalDto) {
        parserProperties.setAutoParsingInterval(autoParsingIntervalDto.autoParsingInterval());
    }

}
