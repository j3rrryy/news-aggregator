package dev.j3rrryy.news_aggregator.service.v1;

import dev.j3rrryy.news_aggregator.config.ParserProperties;
import dev.j3rrryy.news_aggregator.dto.request.AutoParsingIntervalDto;
import dev.j3rrryy.news_aggregator.dto.request.NewsSourceStatusesRequestDto;
import dev.j3rrryy.news_aggregator.dto.response.AutoParsingStatusDto;
import dev.j3rrryy.news_aggregator.dto.response.NewsSourceStatusesResponseDto;
import dev.j3rrryy.news_aggregator.dto.response.ParsingStatusDto;
import dev.j3rrryy.news_aggregator.enums.Source;
import dev.j3rrryy.news_aggregator.exceptions.ParsingInProgressException;
import dev.j3rrryy.news_aggregator.exceptions.ParsingNotRunningException;
import dev.j3rrryy.news_aggregator.mapper.NewsSourceStatusesMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class ParserService {

    private static final AtomicBoolean parsingInProgress = new AtomicBoolean(false);

    private final ParserProperties parserProperties;
    private final ParsingTaskExecutor parsingTaskExecutor;
    private final NewsSourceStatusesMapper newsSourceStatusesMapper;

    public void startParsing() {
        if (!parsingInProgress.compareAndSet(false, true)) {
            throw new ParsingInProgressException();
        }
        parsingTaskExecutor.asyncParsingTask(parsingInProgress);
    }

    public void stopParsing() {
        if (!parsingInProgress.get()) {
            throw new ParsingNotRunningException();
        }
        parsingTaskExecutor.requestStop();
    }


    public ParsingStatusDto getParsingStatus() {
        return new ParsingStatusDto(parsingInProgress.get());
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
