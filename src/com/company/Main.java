package com.company;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.*;
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

    interface StateData {

        class StateList {
           List<State> states = new ArrayList<State>();
        }

        class State {
            String name;
            Integer id;
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

    interface GitHub {

        class Repository {
            String name;
        }

        class Contributor {
            String login;
        }

        @RequestLine("GET /users/{username}/repos?sort=full_name")
        List<Repository> repos(@Param("username") String owner);

        @RequestLine("GET /repos/{owner}/{repo}/contributors")
        List<Contributor> contributors(@Param("owner") String owner, @Param("repo") String repo);

        /**
         * Lists all contributors for all repos owned by a user.
         */
        default List<String> contributors(String owner) {
            return repos(owner).stream()
                    .flatMap(repo -> contributors(owner, repo.name).stream())
                    .map(c -> c.login)
                    .distinct()
                    .collect(Collectors.toList());
        }

        static GitHub connect() {
            ObjectMapper mapper = new ObjectMapper();

            Decoder decoder = new JacksonDecoder(mapper);

            return Feign.builder()
                    .decoder(decoder)
                    .errorDecoder(new GitHubErrorDecoder(decoder))
                    .logger(new Logger.ErrorLogger())
                    .logLevel(Logger.Level.BASIC)
                    .target(GitHub.class, "https://api.github.com");
        }
    }

    static class GitHubClientError extends RuntimeException {
        private String message; // parsed from json

        @Override
        public String getMessage() {
            return message;
        }
    }

    public static void main(String... args) {

        try {
            StateData stateData = StateData.connect();
            StateData.StateList stateList = stateData.allStates();
            System.out.println(stateList);
            for (StateData.State state : stateList.states) {
                System.out.println(state.id + " " + state.name);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if (false) {
            GitHub github = GitHub.connect();

            System.out.println("Let's fetch and print a list of the contributors to this org.");
            List<String> contributors = github.contributors("brisberg");
            for (String contributor : contributors) {
                System.out.println(contributor);
            }

            System.out.println("Now, let's cause an error.");
            try {
                github.contributors("brisberg", "some-unknown-project");
            } catch (GitHubClientError e) {
                System.out.println(e.getMessage());
            }
        }
    }

    static class GitHubErrorDecoder implements ErrorDecoder {

        final Decoder decoder;
        final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

        GitHubErrorDecoder(Decoder decoder) {
            this.decoder = decoder;
        }

        @Override
        public Exception decode(String methodKey, Response response) {
            try {
                return (Exception) decoder.decode(response, GitHubClientError.class);
            } catch (IOException fallbackToDefault) {
                return defaultDecoder.decode(methodKey, response);
            }
        }
    }
}