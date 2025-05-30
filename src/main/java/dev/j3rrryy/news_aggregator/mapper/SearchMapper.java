package dev.j3rrryy.news_aggregator.mapper;

import dev.j3rrryy.news_aggregator.dto.response.NewsArticleFull;
import dev.j3rrryy.news_aggregator.dto.response.NewsArticleSummary;
import dev.j3rrryy.news_aggregator.entity.NewsArticle;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SearchMapper {

    NewsArticleSummary toSummary(NewsArticle newsArticle);

    NewsArticleFull toFull(NewsArticle newsArticle);

}
