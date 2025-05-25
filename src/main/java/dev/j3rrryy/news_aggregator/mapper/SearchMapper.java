package dev.j3rrryy.news_aggregator.mapper;

import dev.j3rrryy.news_aggregator.dto.response.NewsArticleFullDto;
import dev.j3rrryy.news_aggregator.dto.response.NewsArticleSummaryDto;
import dev.j3rrryy.news_aggregator.entity.NewsArticle;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SearchMapper {

    NewsArticleSummaryDto toSummary(NewsArticle newsArticle);

    NewsArticleFullDto toFull(NewsArticle newsArticle);

}
