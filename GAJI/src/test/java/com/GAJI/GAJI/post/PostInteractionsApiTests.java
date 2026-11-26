package com.GAJI.GAJI.post;

import com.GAJI.GAJI.global.auth.SessionAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:gaji-interactions;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostInteractionsApiTests {

    @Autowired MockMvc mvc;
    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.execute("DROP ALL OBJECTS");
        String schema = new ClassPathResource("schema.sql").getContentAsString(StandardCharsets.UTF_8)
                .lines().skip(2).collect(Collectors.joining("\n"));
        var populator = new ResourceDatabasePopulator(new ByteArrayResource(schema.getBytes(StandardCharsets.UTF_8)));
        populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
        populator.execute(dataSource);

        jdbc.update("INSERT INTO location (name, lat, lng) VALUES ('성남', 37.42, 127.12)");
        jdbc.update("""
                INSERT INTO `user` (login_id, password, name, email, location_id, account) VALUES
                ('seller', 'test', '정유성', 'seller@test.com', 1, 'seller_account'),
                ('buyer', 'test', '김민지', 'buyer@test.com', 1, 'buyer_account'),
                ('other', 'test', '이정현', 'other@test.com', 1, 'other_account')
                """);
        jdbc.update("""
                INSERT INTO post (user_id, content, content_img, price, `condition`, deal_status) VALUES
                ('seller', '아이패드 에어', '/uploads/1.jpg', 450000, 'LIKE_NEW', 'ON_SALE'),
                ('seller', '책', NULL, 10000, 'USED', 'SOLD'),
                ('other', '키보드', NULL, 10000, 'NEW', 'RESERVED'),
                ('other', '마우스', NULL, 5000, 'NEW', 'ON_SALE')
                """);
    }

    @Test
    void createsCommentAndListsItWithSessionOwnership() throws Exception {
        mvc.perform(post("/api/posts/1/comments").with(signedIn("buyer")).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"네고 가능할까요?", "userId":"other"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON201"))
                .andExpect(jsonPath("$.message").value("댓글을 작성했습니다."))
                .andExpect(jsonPath("$.result.commentId").value(1));

        mvc.perform(get("/api/posts/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON200"))
                .andExpect(jsonPath("$.message").value("댓글 목록을 조회했습니다."))
                .andExpect(jsonPath("$.result.length()").value(1))
                .andExpect(jsonPath("$.result[0].commentId").value(1))
                .andExpect(jsonPath("$.result[0].userId").value("buyer"))
                .andExpect(jsonPath("$.result[0].userName").value("김민지"))
                .andExpect(jsonPath("$.result[0].content").value("네고 가능할까요?"))
                .andExpect(jsonPath("$.result[0].mine").value(false));
        mvc.perform(get("/api/posts/1/comments").with(signedIn("buyer")))
                .andExpect(jsonPath("$.result[0].mine").value(true));
        mvc.perform(get("/api/posts/1/comments").with(signedIn("other")))
                .andExpect(jsonPath("$.result[0].mine").value(false));
        mvc.perform(get("/api/posts/2/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isEmpty());
    }

    @Test
    void validatesLengthAndListsCommentsInIdOrder() throws Exception {
        for (String content : new String[]{"가", "나".repeat(300)}) {
            mvc.perform(post("/api/posts/1/comments").with(signedIn("buyer")).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"" + content + "\"}"))
                    .andExpect(status().isCreated());
        }
        assertError(mvc.perform(post("/api/posts/1/comments").with(signedIn("buyer"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"" + "가".repeat(301) + "\"}")),
                400, "COMMON400");
        mvc.perform(get("/api/posts/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(2))
                .andExpect(jsonPath("$.result[0].content").value("가"))
                .andExpect(jsonPath("$.result[1].content").value("나".repeat(300)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{", "{}", "{\"content\":null}", "{\"content\":\"\"}",
            "{\"content\":\" \"}", "{\"content\":42}"})
    void rejectsInvalidCommentBody(String body) throws Exception {
        assertError(mvc.perform(post("/api/posts/1/comments").with(signedIn("buyer"))
                .contentType(MediaType.APPLICATION_JSON).content(body)), 400, "COMMON400");
    }

    @Test
    void requiresLoginForCommentChangesAndAnExistingPostForCommentAccess() throws Exception {
        String body = "{\"content\":\"아직 판매 중인가요?\"}";
        assertError(mvc.perform(post("/api/posts/1/comments").contentType(MediaType.APPLICATION_JSON).content(body)),
                401, "COMMON401");
        assertError(mvc.perform(delete("/api/comments/1")), 401, "COMMON401");
        assertError(mvc.perform(post("/api/posts/999/comments").with(signedIn("buyer"))
                .contentType(MediaType.APPLICATION_JSON).content(body)), 404, "POST404_1");
        assertError(mvc.perform(get("/api/posts/999/comments")), 404, "POST404_1");
    }

    @Test
    void onlyAuthorCanDeleteComment() throws Exception {
        jdbc.update("INSERT INTO comment (post_id, user_id, c_content) VALUES (1, 'buyer', '댓글')");
        assertError(mvc.perform(delete("/api/comments/1").with(signedIn("seller"))), 403, "COMMENT403_1");
        mvc.perform(delete("/api/comments/1").with(signedIn("buyer")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON200"))
                .andExpect(jsonPath("$.message").value("댓글을 삭제했습니다."))
                .andExpect(jsonPath("$.result").value(nullValue()));
        assertError(mvc.perform(delete("/api/comments/1").with(signedIn("buyer"))), 404, "COMMENT404_1");
    }

    @Test
    void addsAndCancelsLikeAndReturnsUpdatedCount() throws Exception {
        mvc.perform(post("/api/posts/1/likes").with(signedIn("buyer")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON201"))
                .andExpect(jsonPath("$.message").value("찜했습니다."))
                .andExpect(jsonPath("$.result.likesCount").value(1));
        assertError(mvc.perform(post("/api/posts/1/likes").with(signedIn("buyer"))), 409, "LIKE409_1");
        assertError(mvc.perform(delete("/api/posts/1/likes").with(signedIn("other"))), 404, "LIKE404_1");
        mvc.perform(get("/api/posts/1").with(signedIn("buyer")))
                .andExpect(jsonPath("$.result.likedByMe").value(true))
                .andExpect(jsonPath("$.result.likesCount").value(1));

        mvc.perform(delete("/api/posts/1/likes").with(signedIn("buyer")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON200"))
                .andExpect(jsonPath("$.message").value("찜을 취소했습니다."))
                .andExpect(jsonPath("$.result.likesCount").value(0));
        assertError(mvc.perform(delete("/api/posts/1/likes").with(signedIn("buyer"))), 404, "LIKE404_1");
        mvc.perform(get("/api/posts/1").with(signedIn("buyer")))
                .andExpect(jsonPath("$.result.likedByMe").value(false))
                .andExpect(jsonPath("$.result.likesCount").value(0));
    }

    @Test
    void requiresSessionAndReportsMissingPostOrLike() throws Exception {
        assertError(mvc.perform(post("/api/posts/1/likes")), 401, "COMMON401");
        assertError(mvc.perform(delete("/api/posts/1/likes")), 401, "COMMON401");
        assertError(mvc.perform(get("/api/users/me/likes")), 401, "COMMON401");
        assertError(mvc.perform(post("/api/posts/999/likes").with(signedIn("buyer"))), 404, "POST404_1");
        assertError(mvc.perform(delete("/api/posts/999/likes").with(signedIn("buyer"))), 404, "LIKE404_1");
    }

    @Test
    void likedPostsUseSessionAndMatchPostSummaryContract() throws Exception {
        seedLikes();
        mvc.perform(get("/api/users/me/likes").with(signedIn("buyer")).param("userId", "other"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON200"))
                .andExpect(jsonPath("$.message").value("찜한 게시글을 조회했습니다."))
                .andExpect(jsonPath("$.result.page").value(0))
                .andExpect(jsonPath("$.result.size").value(20))
                .andExpect(jsonPath("$.result.hasNext").value(false))
                .andExpect(jsonPath("$.result.items.length()").value(3))
                .andExpect(jsonPath("$.result.items[0].postId").value(3))
                .andExpect(jsonPath("$.result.items[2].postId").value(1))
                .andExpect(jsonPath("$.result.items[2].sellerId").value("seller"))
                .andExpect(jsonPath("$.result.items[2].sellerName").value("정유성"))
                .andExpect(jsonPath("$.result.items[2].content").value("아이패드 에어"))
                .andExpect(jsonPath("$.result.items[2].contentImg").value("/uploads/1.jpg"))
                .andExpect(jsonPath("$.result.items[2].price").value(450000))
                .andExpect(jsonPath("$.result.items[2].condition").value("LIKE_NEW"))
                .andExpect(jsonPath("$.result.items[2].dealStatus").value("ON_SALE"))
                .andExpect(jsonPath("$.result.items[2].likesCount").value(2));
    }

    @ParameterizedTest
    @CsvSource({"latest,3,2,1", "priceAsc,3,2,1", "priceDesc,1,3,2", "likes,3,1,2"})
    void likedPostsShareSortAndPaginationWithPostList(String sort, int first, int second, int third) throws Exception {
        seedLikes();
        mvc.perform(get("/api/users/me/likes").with(signedIn("buyer")).param("sort", sort).param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items.length()").value(2))
                .andExpect(jsonPath("$.result.items[0].postId").value(first))
                .andExpect(jsonPath("$.result.items[1].postId").value(second))
                .andExpect(jsonPath("$.result.hasNext").value(true));
        mvc.perform(get("/api/users/me/likes").with(signedIn("buyer"))
                        .param("sort", sort).param("size", "2").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items.length()").value(1))
                .andExpect(jsonPath("$.result.items[0].postId").value(third))
                .andExpect(jsonPath("$.result.hasNext").value(false));
    }

    @Test
    void returnsEmptyLikedPostsAndRejectsInvalidSortOrPage() throws Exception {
        mvc.perform(get("/api/users/me/likes").with(signedIn("buyer")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items").isEmpty())
                .andExpect(jsonPath("$.result.hasNext").value(false));
        assertError(mvc.perform(get("/api/users/me/likes").with(signedIn("buyer")).param("sort", "invalid")),
                400, "COMMON400");
        assertError(mvc.perform(get("/api/users/me/likes").with(signedIn("buyer")).param("page", "-1")),
                400, "COMMON400");
    }

    @Test
    void rollsBackLikeChangesWhenCounterUpdateFails() throws Exception {
        jdbc.update("UPDATE post SET likes_count = ? WHERE id = 1", Integer.MAX_VALUE);
        assertError(mvc.perform(post("/api/posts/1/likes").with(signedIn("buyer"))), 500, "COMMON500");
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM likes WHERE post_id = 1", Integer.class));

        jdbc.update("INSERT INTO likes (user_id, post_id) VALUES ('buyer', 2)");
        assertError(mvc.perform(delete("/api/posts/2/likes").with(signedIn("buyer"))), 500, "COMMON500");
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM likes WHERE post_id = 2", Integer.class));
    }

    @Test
    void concurrentRequestsCountDistinctLikesExactlyOnce() throws Exception {
        var ready = new CountDownLatch(3);
        try (var executor = Executors.newFixedThreadPool(3)) {
            List<Callable<Integer>> requests = List.of("buyer", "buyer", "other").stream()
                    .<Callable<Integer>>map(userId -> () -> {
                        ready.countDown();
                        if (!ready.await(5, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("찜 요청이 동시에 시작되지 않았습니다.");
                        }
                        return mvc.perform(post("/api/posts/1/likes").with(signedIn(userId)))
                                .andReturn().getResponse().getStatus();
                    }).toList();
            var responses = executor.invokeAll(requests, 10, TimeUnit.SECONDS);
            int first = responses.get(0).get();
            int second = responses.get(1).get();
            assertEquals(201, Math.min(first, second));
            assertEquals(409, Math.max(first, second));
            assertEquals(201, responses.get(2).get());
        }
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM likes WHERE post_id = 1", Integer.class));
        assertEquals(2, jdbc.queryForObject("SELECT likes_count FROM post WHERE id = 1", Integer.class));
    }

    private void seedLikes() {
        jdbc.update("""
                INSERT INTO likes (user_id, post_id) VALUES
                ('buyer', 1), ('buyer', 2), ('buyer', 3), ('other', 3), ('other', 4), ('seller', 1)
                """);
        jdbc.update("UPDATE post SET likes_count = 2 WHERE id IN (1, 3)");
        jdbc.update("UPDATE post SET likes_count = 1 WHERE id IN (2, 4)");
    }

    private static RequestPostProcessor signedIn(String userId) {
        return request -> {
            SessionAuth.login(request, userId);
            return request;
        };
    }

    private static void assertError(ResultActions result, int httpStatus, String code) throws Exception {
        result.andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.result").value(nullValue()));
    }
}
