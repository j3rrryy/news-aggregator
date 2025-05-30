package dev.j3rrryy.news_aggregator.service.v1;

import dev.j3rrryy.news_aggregator.dto.request.AutoParsingInterval;
import dev.j3rrryy.news_aggregator.dto.request.NewsSourceStatusesRequest;
import dev.j3rrryy.news_aggregator.dto.response.AutoParsingStatus;
import dev.j3rrryy.news_aggregator.dto.response.NewsSourceStatusesResponse;
import dev.j3rrryy.news_aggregator.dto.response.ParsingStatus;
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


    public ParsingStatus getParsingStatus() {
        return new ParsingStatus(parsingStatusManager.isParsingInProgress());
    }

    public NewsSourceStatusesResponse getSourceStatuses() {
        return newsSourceStatusesMapper.toResponseDto(parserProperties.getSourceStatuses());
    }

    public void patchSourceStatuses(NewsSourceStatusesRequest dto) {
        Map<Source, Boolean> currentStatus = parserProperties.getSourceStatuses();
        newsSourceStatusesMapper.updateStatusMap(dto, currentStatus);
        parserProperties.setSourceStatuses(currentStatus);
    }

    public AutoParsingStatus getAutoParsingStatus() {
        return new AutoParsingStatus(
                parserProperties.isAutoParsingEnabled(),
                parserProperties.getAutoParsingInterval()
        );
    }

    public void setAutoParsingInterval(AutoParsingInterval dto) {
        parserProperties.setAutoParsingInterval(dto.autoParsingInterval());
    }

}
