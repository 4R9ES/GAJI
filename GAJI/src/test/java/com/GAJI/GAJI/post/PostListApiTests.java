package com.GAJI.GAJI.post;

import com.GAJI.GAJI.global.auth.SessionAuth;
import com.GAJI.GAJI.global.exception.GlobalExceptionHandler;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Statement;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 실제 MyBatis SQL과 HTTP 응답을 검증한다. 개발용 MySQL 데이터에는 접근하지 않는다. */
class PostListApiTests {

    private SqlSession session;
    private MockMvc mvc;

    @TempDir
    Path uploads;

    @BeforeEach
    void setUp() throws Exception {
        var dataSource = new UnpooledDataSource("org.h2.Driver",
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", "");
        var environment = new Environment("test", new JdbcTransactionFactory(), dataSource);
        var configuration = new Configuration(environment);
        configuration.addMapper(PostMapper.class);
        session = new SqlSessionFactoryBuilder().build(configuration).openSession();

        try (Statement statement = session.getConnection().createStatement()) {
            statement.execute("""
                    CREATE TABLE location (location_id INT PRIMARY KEY, name VARCHAR(100) NOT NULL)
                    """);
            statement.execute("""
                    CREATE TABLE `user` (
                        login_id VARCHAR(20) PRIMARY KEY, name VARCHAR(50) NOT NULL,
                        user_rank TINYINT NOT NULL, location_id INT NOT NULL,
                        FOREIGN KEY (location_id) REFERENCES location(location_id)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE post (
                        id INT AUTO_INCREMENT PRIMARY KEY, user_id VARCHAR(20) NOT NULL,
                        content VARCHAR(1000) NOT NULL, content_img VARCHAR(255),
                        price INT NOT NULL, `condition` ENUM('NEW', 'LIKE_NEW', 'USED') NOT NULL,
                        deal_status ENUM('ON_SALE', 'RESERVED', 'SOLD') NOT NULL DEFAULT 'ON_SALE',
                        likes_count INT NOT NULL DEFAULT 0,
                        FOREIGN KEY (user_id) REFERENCES `user`(login_id)
                    )
                    """);
            statement.execute("""
                    INSERT INTO location VALUES (1, '서울'), (2, '경기도 화성시 동탄1동')
                    """);
            statement.execute("""
                    INSERT INTO `user` VALUES
                    ('yuseong01', '정유성', 50, 1), ('minsu02', '김민수', 54, 2)
                    """);
            statement.execute("""
                    INSERT INTO post VALUES
                    (1, 'yuseong01', '게시물 1', NULL, 300, 'NEW', 'ON_SALE', 5),
                    (2, 'minsu02', '게시물 2', NULL, 100, 'USED', 'SOLD', 1),
                    (3, 'yuseong01', '게시물 3', NULL, 100, 'NEW', 'ON_SALE', 5),
                    (4, 'minsu02', '아이패드 팝니다', '/uploads/4.jpg', 200, 'LIKE_NEW', 'RESERVED', 7)
                    """);
            statement.execute("""
                    CREATE TABLE likes (
                        user_id VARCHAR(20) NOT NULL, post_id INT NOT NULL,
                        PRIMARY KEY (user_id, post_id)
                    )
                    """);
            statement.execute("INSERT INTO likes VALUES ('yuseong01', 4), ('minsu02', 3)");
        }
        var service = new PostService(session.getMapper(PostMapper.class), new PostImageStorage(uploads.toString()));
        mvc = MockMvcBuilders.standaloneSetup(new PostController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        if (session != null) {
            session.close();
        }
    }

    @Test
    void defaultsAndSummaryMatchContractWithoutLogin() throws Exception {
        mvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON200"))
                .andExpect(jsonPath("$.message").value("게시글 목록을 조회했습니다."))
                .andExpect(jsonPath("$.result.page").value(0))
                .andExpect(jsonPath("$.result.size").value(20))
                .andExpect(jsonPath("$.result.hasNext").value(false))
                .andExpect(jsonPath("$.result.items.length()").value(4))
                .andExpect(jsonPath("$.result.items[0].postId").value(4))
                .andExpect(jsonPath("$.result.items[0].sellerId").value("minsu02"))
                .andExpect(jsonPath("$.result.items[0].sellerName").value("김민수"))
                .andExpect(jsonPath("$.result.items[0].content").value("아이패드 팝니다"))
                .andExpect(jsonPath("$.result.items[0].contentImg").value("/uploads/4.jpg"))
                .andExpect(jsonPath("$.result.items[0].price").value(200))
                .andExpect(jsonPath("$.result.items[0].condition").value("LIKE_NEW"))
                .andExpect(jsonPath("$.result.items[0].dealStatus").value("RESERVED"))
                .andExpect(jsonPath("$.result.items[0].likesCount").value(7));
    }

    @ParameterizedTest
    @CsvSource({"latest,4,3,2,1", "priceAsc,3,2,4,1", "priceDesc,1,4,3,2", "likes,4,3,1,2"})
    void sortsWithDescendingIdAsTieBreaker(String sort, int first, int second, int third, int fourth) throws Exception {
        mvc.perform(get("/api/posts").param("sort", sort))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items.length()").value(4))
                .andExpect(jsonPath("$.result.items[0].postId").value(first))
                .andExpect(jsonPath("$.result.items[1].postId").value(second))
                .andExpect(jsonPath("$.result.items[2].postId").value(third))
                .andExpect(jsonPath("$.result.items[3].postId").value(fourth));
    }

    @Test
    void filtersSellerBeforePagination() throws Exception {
        mvc.perform(get("/api/posts").param("userId", "yuseong01").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items.length()").value(1))
                .andExpect(jsonPath("$.result.items[0].postId").value(3))
                .andExpect(jsonPath("$.result.items[0].sellerName").value("정유성"))
                .andExpect(jsonPath("$.result.hasNext").value(true));
        mvc.perform(get("/api/posts").param("userId", "yuseong01").param("size", "1").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items.length()").value(1))
                .andExpect(jsonPath("$.result.items[0].postId").value(1))
                .andExpect(jsonPath("$.result.hasNext").value(false));
    }

    @ParameterizedTest
    @CsvSource({"0,2,true", "1,2,false", "2,0,false"})
    void paginatesAndDetectsExactLastPage(int page, int count, boolean hasNext) throws Exception {
        mvc.perform(get("/api/posts").param("page", String.valueOf(page)).param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.page").value(page))
                .andExpect(jsonPath("$.result.size").value(2))
                .andExpect(jsonPath("$.result.items.length()").value(count))
                .andExpect(jsonPath("$.result.hasNext").value(hasNext));
    }

    @Test
    void nonexistentSellerReturnsEmptyPage() throws Exception {
        mvc.perform(get("/api/posts").param("userId", "missing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items").isEmpty())
                .andExpect(jsonPath("$.result.hasNext").value(false));
    }

    @Test
    void userIdCannotInjectSql() throws Exception {
        mvc.perform(get("/api/posts").param("userId", "' OR '1'='1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items").isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"sort,invalid", "sort,priceAsc; DROP TABLE post", "page,-1", "size,0", "size,-1",
            "page,abc", "size,abc", "page,2147483648"})
    void invalidParametersReturnCommon400(String parameter, String value) throws Exception {
        mvc.perform(get("/api/posts").param(parameter, value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON400"));
    }

    @Test
    void largePaginationDoesNotOverflow() throws Exception {
        mvc.perform(get("/api/posts").param("page", "2147483647").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items").isEmpty())
                .andExpect(jsonPath("$.result.hasNext").value(false));
    }

    @Test
    void detailsIncludeSellerAndDefaultToFalseForGuests() throws Exception {
        mvc.perform(get("/api/posts/4").param("userId", "minsu02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON200"))
                .andExpect(jsonPath("$.message").value("게시글을 조회했습니다."))
                .andExpect(jsonPath("$.result.postId").value(4))
                .andExpect(jsonPath("$.result.seller.loginId").value("minsu02"))
                .andExpect(jsonPath("$.result.seller.name").value("김민수"))
                .andExpect(jsonPath("$.result.seller.userRank").value(54))
                .andExpect(jsonPath("$.result.seller.locationName").value("경기도 화성시 동탄1동"))
                .andExpect(jsonPath("$.result.content").value("아이패드 팝니다"))
                .andExpect(jsonPath("$.result.contentImg").value("/uploads/4.jpg"))
                .andExpect(jsonPath("$.result.price").value(200))
                .andExpect(jsonPath("$.result.condition").value("LIKE_NEW"))
                .andExpect(jsonPath("$.result.dealStatus").value("RESERVED"))
                .andExpect(jsonPath("$.result.likesCount").value(7))
                .andExpect(jsonPath("$.result.likedByMe").value(false))
                .andExpect(jsonPath("$.result.mine").value(false));
    }

    @ParameterizedTest
    @CsvSource({"yuseong01,4,true,false", "minsu02,4,false,true", "yuseong01,2,false,false"})
    void detailFlagsUseTheSessionAndRequestedPost(String loginId, int postId, boolean liked, boolean mine)
            throws Exception {
        mvc.perform(get("/api/posts/{postId}", postId).with(request -> {
                    SessionAuth.login(request, loginId);
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.likedByMe").value(liked))
                .andExpect(jsonPath("$.result.mine").value(mine));
    }

    @Test
    void missingPostReturnsPost404() throws Exception {
        mvc.perform(get("/api/posts/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("POST404_1"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 게시글입니다."))
                .andExpect(jsonPath("$.result").hasJsonPath())
                .andExpect(jsonPath("$.result").isEmpty());
    }

    @Test
    void createsPostWithoutImageUsingSessionAndDatabaseDefaults() throws Exception {
        mvc.perform(createRequest().param("sellerId", "yuseong01"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON201"))
                .andExpect(jsonPath("$.message").value("판매글을 등록했습니다."))
                .andExpect(jsonPath("$.result.postId").value(5));
        mvc.perform(get("/api/posts/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.seller.loginId").value("minsu02"))
                .andExpect(jsonPath("$.result.content").value("새 판매글"))
                .andExpect(jsonPath("$.result.price").value(450000))
                .andExpect(jsonPath("$.result.condition").value("LIKE_NEW"))
                .andExpect(jsonPath("$.result.contentImg").isEmpty())
                .andExpect(jsonPath("$.result.dealStatus").value("ON_SALE"))
                .andExpect(jsonPath("$.result.likesCount").value(0));
    }

    @ParameterizedTest
    @CsvSource({"png,image/png", "jpg,image/jpeg"})
    void savesImageAndStoresPublicPath(String format, String contentType) throws Exception {
        byte[] image = imageBytes(format);
        var request = createRequest().file(new MockMultipartFile("image", "original." + format, contentType, image));
        mvc.perform(request).andExpect(status().isCreated());

        Path saved;
        try (var files = Files.list(uploads)) {
            var images = files.toList();
            assertEquals(1, images.size());
            saved = images.getFirst();
        }
        assertArrayEquals(image, Files.readAllBytes(saved));
        mvc.perform(get("/api/posts/5"))
                .andExpect(jsonPath("$.result.contentImg").value("/uploads/" + saved.getFileName()));
    }

    @Test
    void requiresLoginBeforeValidatingFields() throws Exception {
        mvc.perform(multipart("/api/posts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON401"));
    }

    @ParameterizedTest
    @CsvSource({"'',450000,NEW", "내용,-1,NEW", "내용,1.5,NEW", "내용,450000,INVALID", "내용,,NEW"})
    void rejectsInvalidCreateFields(String content, String price, String condition) throws Exception {
        var request = multipart("/api/posts");
        request.param("content", content).param("condition", condition).with(servletRequest -> {
            SessionAuth.login(servletRequest, "minsu02");
            return servletRequest;
        });
        if (price != null) {
            request.param("price", price);
        }
        mvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400"));
    }

    @Test
    void rejectsDisguisedImageAndMultipleImages() throws Exception {
        var fakeImage = new MockMultipartFile("image", "fake.png", "image/png", new byte[]{1, 2, 3});
        var realImage = new MockMultipartFile("image", "real.png", "image/png", imageBytes("png"));
        for (var request : new MockMultipartHttpServletRequestBuilder[]{
                createRequest().file(fakeImage), createRequest().file(realImage).file(realImage)}) {
            mvc.perform(request)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON400"));
        }
    }

    @Test
    void removesImageIfDatabaseInsertFails() throws Exception {
        var image = new MockMultipartFile("image", "real.png", "image/png", imageBytes("png"));
        var request = createRequest().file(image);
        request.with(servletRequest -> {
            SessionAuth.login(servletRequest, "unknown-user");
            return servletRequest;
        });
        mvc.perform(request)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("COMMON500"));
        try (var files = Files.list(uploads)) {
            assertEquals(0, files.count());
        }
    }

    private MockMultipartHttpServletRequestBuilder createRequest() {
        var request = multipart("/api/posts");
        request.param("content", "새 판매글").param("price", "450000").param("condition", "LIKE_NEW")
                .with(servletRequest -> {
                    SessionAuth.login(servletRequest, "minsu02");
                    return servletRequest;
                });
        return request;
    }

    private byte[] imageBytes(String format) throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), format, output);
        return output.toByteArray();
    }
}
