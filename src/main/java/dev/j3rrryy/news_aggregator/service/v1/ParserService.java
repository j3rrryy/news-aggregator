package dev.j3rrryy.news_aggregator.service.v1;

import dev.j3rrryy.news_aggregator.dto.request.AutoParsingIntervalDto;
import dev.j3rrryy.news_aggregator.dto.request.NewsSourceStatusesRequestDto;
import dev.j3rrryy.news_aggregator.dto.response.AutoParsingStatusDto;
import dev.j3rrryy.news_aggregator.dto.response.NewsSourceStatusesResponseDto;
import dev.j3rrryy.news_aggregator.dto.response.ParsingStatusDto;
import dev.j3rrryy.news_aggregator.enums.Source;
import dev.j3rrryy.news_aggregator.exceptions.ParsingInProgressException;
import dev.j3rrryy.news_aggregator.exceptions.ParsingNotRunningException;
import dev.j3rrryy.news_aggregator.mapper.NewsSourceStatusesMapper;
import dev.j3rrryy.news_aggregator.parser.config.ParserProperties;
import dev.j3rrryy.news_aggregator.parser.service.ParsingOrchestrator;
import dev.j3rrryy.news_aggregator.parser.service.ParsingStatusManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ParserService {

    private final ParserProperties parserProperties;
    private final ParsingOrchestrator parsingOrchestrator;
    private final ParsingStatusManager parsingStatusManager;
    private final NewsSourceStatusesMapper newsSourceStatusesMapper;

    public void startParsing() {
        if (!parsingStatusManager.startParsing()) {
            throw new ParsingInProgressException();
        }
        parsingOrchestrator.runAsyncParsing();
    }

    public void stopParsing() {
        if (!parsingStatusManager.isParsingInProgress()) {
            throw new ParsingNotRunningException();
        }
        parsingStatusManager.requestStop();
    }


    public ParsingStatusDto getParsingStatus() {
        return new ParsingStatusDto(parsingStatusManager.isParsingInProgress());
    }

    public NewsSourceStatusesResponseDto getSourceStatuses() {
        return newsSourceStatusesMapper.toResponseDto(parserProperties.getSourceStatuses());
    }

    public void patchSourceStatuses(NewsSourceStatusesRequestDto newsSourceStatusesRequestDto) {
        Map<Source, Boolean> currentStatus = parserProperties.getSourceStatuses();
        newsSourceStatusesMapper.updateStatusMap(newsSourceStatusesRequestDto, currentStatus);
        parserProperties.setSourceStatuses(currentStatus);
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
