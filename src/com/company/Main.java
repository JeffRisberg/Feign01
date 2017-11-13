package com.company;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.Headers;
import feign.Logger;
import feign.Param;
import feign.RequestLine;
import feign.Response;
import feign.codec.Decoder;
import feign.codec.ErrorDecoder;
import feign.jackson.JacksonDecoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Inspired by {@code com.example.retrofit.GitHubClient}
 */
public class Main {

    @Headers("Accept: application/json")
    interface StateData {

        @JsonIgnoreProperties(ignoreUnknown = true)
        class StateList {
            List<State> country_fact_sheets = new ArrayList<State>();
            String api_version;
            Integer page;
            Integer per_page;

            public List<State> getCountry_fact_sheets() {
                return country_fact_sheets;
            }

            public void setCountry_fact_sheets(List<State> country_fact_sheets) {
                this.country_fact_sheets = country_fact_sheets;
            }

            public String getApi_version() {
                return api_version;
            }

            public void setApi_version(String api_version) {
                this.api_version = api_version;
            }

            public Integer getPage() {
                return page;
            }

            public void setPage(Integer page) {
                this.page = page;
            }

            public Integer getPer_page() {
                return per_page;
            }

            public void setPer_page(Integer per_page) {
                this.per_page = per_page;
            }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        class State {
            Integer id;
            String title;

            public Integer getId() {
                return id;
            }

            public void setId(Integer id) {
                this.id = id;
            }

            public String getTitle() {
                return title;
            }

            public void setTitle(String title) {
                this.title = title;
            }
        }

        @RequestLine("GET /api/v1/?command=get_country_fact_sheets&page=0")
        StateList states();

        /**
         * Lists all states
         */
        default StateList allStates() {
            return states();
        }

        static StateData connect() {
            ObjectMapper mapper = new ObjectMapper();

            return Feign.builder()
                    .decoder(new JacksonDecoder(mapper))
                    .logger(new Logger.ErrorLogger())
                    .logLevel(Logger.Level.BASIC)
                    .target(StateData.class, "https://www.state.gov");
        }
    }

    public static void main(String... args) {

        try {
            StateData stateData = StateData.connect();
            StateData.StateList stateList = stateData.allStates();
            System.out.println(stateList);
            for (StateData.State state : stateList.country_fact_sheets) {
                System.out.println(state.id + " " + state.title);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}