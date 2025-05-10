package dev.j3rrryy.news_aggregator.entity;

import dev.j3rrryy.news_aggregator.enums.Category;
import dev.j3rrryy.news_aggregator.enums.Source;
import dev.j3rrryy.news_aggregator.enums.Status;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@Table(name = "news_articles")
@NoArgsConstructor(force = true)
public class NewsArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    private String title;

    @NotNull
    private String summary;

    @NotNull
    private String content;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private Category category;

    @Column(name = "keyword")
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "news_keywords",
            joinColumns = @JoinColumn(name = "article_id")
    )
    private Set<String> keywords;

    @Column(name = "media_url")
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "news_media_urls",
            joinColumns = @JoinColumn(name = "article_id")
    )
    private Set<String> mediaUrls;

    @NotNull
    private String url;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private Status status;

    @NotNull
    private LocalDateTime publishedAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private Source source;

}
