package github.api;

import com.google.gson.*;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.*;


public class GitHubHandler {
    private String TOKEN;
    final private String startDate = "2009-08-07";
    final private String endDate = "2009-08-14";

    public GitHubHandler(String token) {TOKEN = token;}

    public Repository[] getTop10StarredRepos() throws Exception {
        String url = "https://api.github.com/search/repositories?q=stars:%3E=1&sort=stars&order=desc";
        String jsonResult = getJsonResult(url);
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = (JsonObject)jsonParser.parse(jsonResult);
        JsonArray jsonRepos = jsonObject.getAsJsonArray("items");

        int neededReposCount = 10;
        Repository[] repos = new Repository[neededReposCount];
        JsonObject currentRepoJson;
        for (int i = 0; i < repos.length; i++) {
            currentRepoJson = (JsonObject) jsonRepos.get(i);
            repos[i] = convertToRepo(currentRepoJson);
            repos[i].setStarsCount(getStarsCount(currentRepoJson));
        }

        return repos;
    }

    public Repository[] getTop10CommitedRepositoriesInWeek(int pagesCount) throws Exception {
        List<Repository> allRepos = getAllReposInWeek(pagesCount);
        allRepos.sort(Comparator.comparing(Repository::getCommitsCount).reversed());
        int neededReposCount = 10;
        Repository[] repos = new Repository[neededReposCount];
        for (int i = 0; i < neededReposCount; i++)
            repos[i] = allRepos.get(i);
        return repos;
    }

    private List<Repository> getAllReposInWeek(int pagesCount) throws Exception {
        List<Repository> allRepos = new ArrayList<>();
        int page = 1;
        while(page <= pagesCount) {
            StringBuilder url = new StringBuilder();
            url.append("https://api.github.com/search/repositories?q=created:")
                    .append(startDate).append("..").append(endDate)
                    .append("&page=").append(String.valueOf(page));
            String jsonResult = getJsonResult(url.toString());
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = (JsonObject) jsonParser.parse(jsonResult);
            JsonArray jsonRepos = jsonObject.getAsJsonArray("items");
            if (jsonRepos.size() == 0)
                break;

            Repository currentRepo;
            for (int i = 0; i < jsonRepos.size(); i++) {
                currentRepo = convertToRepo((JsonObject) jsonRepos.get(i));
                currentRepo.setCommitsCount(getContributorsCommitsCount((JsonObject) jsonRepos.get(i)));
                allRepos.add(currentRepo);
            }
            page++;
        }
        return allRepos;
    }

    private int getContributorsCommitsCount(JsonObject jsonRepo) throws Exception {
        int contributionsCommitsCount = 0;
        int page = 1;
        final int pageCountFor60Contributors = 2;
        StringBuilder url;
        while (page <= pageCountFor60Contributors) {
            url = new StringBuilder(jsonRepo.get("contributors_url").getAsString());
            url.append("?page=").append(page);
            String jsonResult = getJsonResult(url.toString());
            if (jsonResult == null)
                return 0;

            JsonParser jsonParser = new JsonParser();
            JsonArray jsonContributors = (JsonArray) jsonParser.parse(jsonResult);
            if (jsonContributors.size() == 0)
                break;

            for (int i = 0; i < jsonContributors.size(); i++)
                contributionsCommitsCount += getContributorCommits((JsonObject) jsonContributors.get(i));
            page++;
        }
        return contributionsCommitsCount;
    }

    private String getJsonResult(String url) throws IOException, IllegalArgumentException {
        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "token " + TOKEN);
        request.setHeader("Accept", "application/vnd.github.nightshade-preview+json");
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = client.execute(request);
        String jsonResult = EntityUtils.toString(response.getEntity(), "UTF-8");
        client.close();
        return jsonResult;
    }

    public ContributorToRepo[] getTop5ReposContributors(Repository repo) throws Exception {
        StringBuilder url = new StringBuilder();
        url.append("https://api.github.com/repos/").append(repo.getName()).append("/contributors");
        String jsonResult = getJsonResult(url.toString());
        JsonParser jsonParser = new JsonParser();
        JsonArray jsonContributors = (JsonArray)jsonParser.parse(jsonResult);

        int neededCotributorsCount = jsonContributors.size() >= 5 ? 5 : jsonContributors.size();
        ContributorToRepo[] contributors = new ContributorToRepo[neededCotributorsCount];
        for (int i = 0; i < contributors.length; i++)
            contributors[i] = convertToCommitter((JsonObject)jsonContributors.get(i));

        return contributors;
    }

    private ContributorToRepo convertToCommitter(JsonObject jsonContributor) {
        String name = jsonContributor.get("login").getAsString();
        int commitsCount = jsonContributor.get("contributions").getAsInt();
        return new ContributorToRepo(name, commitsCount);
    }
    private int getContributorCommits(JsonObject jsonContributor) {
        return jsonContributor.get("contributions").getAsInt();
    }

    private Repository convertToRepo(JsonObject jsonRepo) {
        String name = jsonRepo.get("full_name").getAsString();
        String description = jsonRepo.get("description").isJsonNull() ? "" : jsonRepo.get("description").getAsString();
        String language = jsonRepo.get("language").isJsonNull() ? "" : jsonRepo.get("language").getAsString();
        return new Repository(name, description, language);
    }
    private int getStarsCount(JsonObject jsonRepo) {
        return jsonRepo.get("stargazers_count").getAsInt();
    }
}
