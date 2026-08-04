package com.example.darks.repair_auto.repair.request.infrastructure;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RepairRequestNumberGenerator {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    @Autowired
    public RepairRequestNumberGenerator(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, Clock.systemUTC());
    }

    RepairRequestNumberGenerator(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    public String nextRequestNumber() {
        Long sequenceValue = jdbcTemplate.queryForObject("select nextval('repair_request_number_seq')", Long.class);
        int year = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).getYear();
        return "REP-%d-%06d".formatted(year, sequenceValue);
    }
}
