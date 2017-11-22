package example.github.api;

import github.api.Repository;

import org.junit.*;
import static org.junit.Assert.*;


public class RepositoryTest {
    @Test
    public void repositoryTest() {
        Repository repo = new Repository("The Repo", "TestRepo", "Java");
        assertEquals("\nname: The Repo\ndescription: TestRepo\nlanguage: Java", repo.toString());

        repo.setCommitsCount(0);
        assertNotNull(repo.getCommitsCount());
        assertFalse(repo.toString().contains("commitsCount"));
        repo.setCommitsCount(1);
        assertTrue(repo.toString().contains("commitsCount"));

        repo.setStarsCount(0);
        assertEquals(0, repo.getStarsCount());
        assertFalse(repo.toString().contains("starsCount"));
        repo.setStarsCount(1);
        assertTrue(repo.toString().contains("starsCount"));
    }
}