package com.igot.cb.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
@Getter
@Setter
public class CbServerProperties {
  @Value("${recent.searches.limit}")
  private Integer recentSearchesLimit;
}
