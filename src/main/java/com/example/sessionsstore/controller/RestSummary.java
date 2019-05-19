package com.example.sessionsstore.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
class RestSummary {

   private Integer totalCount;
   private Integer startedCount;
   private Integer stoppedCount;

}
