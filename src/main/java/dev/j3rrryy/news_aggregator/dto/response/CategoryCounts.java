package dev.j3rrryy.news_aggregator.dto.response;

import java.io.Serializable;

public record CategoryCounts(
        int politics, int economics, int society, int sport, int scienceTech
) implements Serializable {

}
