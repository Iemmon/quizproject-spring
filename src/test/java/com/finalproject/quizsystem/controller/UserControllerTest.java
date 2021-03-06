package com.finalproject.quizsystem.controller;

import com.finalproject.quizsystem.entity.*;
import com.finalproject.quizsystem.service.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.security.Principal;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    public MockMvc mvc;

    @MockBean
    private ResultService resultService;

    @MockBean
    private UserService userService;

    @MockBean
    private TopicService topicService;

    @MockBean
    private QuizService quizService;

    @MockBean
    private QuestionService questionService;

    @MockBean
    private MailSender mailSender;

    @Test
    @WithMockUser(roles = "STUDENT")
    public void getResultsShouldRedirectToResultsPage() throws Exception {

        User user = mock(User.class);
        when(userService.loadUserByUsername(anyString())).thenReturn(user);

        Page<Result> userResults = mock(Page.class);
        when(resultService.getAllResults(anyLong(), any(Pageable.class))).thenReturn(userResults);

        mvc.perform(get("/user/results"))
                .andExpect(status().isOk())
                .andExpect(view().name("userresults"))
                .andExpect(model().attribute("results", userResults));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    public void chooseTopicShouldRedirectToTopicsPage() throws Exception {
        List<Topic> topics = new ArrayList<>();
        when(topicService.findAll()).thenReturn(topics);

        mvc.perform(get("/user/topics"))
                .andExpect(status().isOk())
                .andExpect(view().name("topics"))
                .andExpect(model().attribute("topics", topics));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    public void chooseQuizShouldRedirectToQuizesPage() throws Exception {
        List<Quiz> quizList = new ArrayList<>();
        when(quizService.findAllByTopicId(anyLong())).thenReturn(quizList);

        mvc.perform(get("/user/quizes").param("topic_id", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("quizes"))
                .andExpect(model().attribute("quizes", quizList));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    public void startQuizShouldRedirectToQuestionPage() throws Exception {
        List<Question> questionList = new ArrayList<>();
        when(questionService.findAllByTestId(anyLong())).thenReturn(questionList);

        mvc.perform(get("/user/questions").param("quiz_id", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("questions"))
                .andExpect(model().attribute("questions", questionList));
    }

    @Test
    @WithMockUser(username="user@gmail.com", roles="STUDENT")
    public void getResultShouldRedirectToResultPage() throws Exception {

        String username = "user@gmail.com";
        Long quizId = 5L;
        List<Long> results = Arrays.asList(5L, 10L);
        Set<Long> selected = new HashSet<>(results);
        Integer score = 100;

        List<Question> checkedQuestions = new ArrayList<>();
        when(questionService.getIncorrectAnsweredQuestions(anyLong(), anySet())).thenReturn(checkedQuestions);

        Quiz quiz = Quiz.builder().questions(Collections.singletonList(Question.builder().build())).build();
        when( quizService.findById(anyLong())).thenReturn(Optional.of(quiz));

        User currentUser = User.builder().email(username).id(5L).build();
        when(userService.loadUserByUsername(anyString())).thenReturn(currentUser);

        Result result = Result.builder().score(score).user(currentUser).quiz(quiz).build();

        mvc.perform(post("/user/result").with(csrf())
                .param("result", Long.toString(results.get(0)))
                .param("result", Long.toString(results.get(1)))
                .param("quiz_id", Long.toString(quizId)))
            .andExpect(status().isOk())
            .andExpect(model().attribute("questions", checkedQuestions))
            .andExpect(model().attribute("selected", selected))
            .andExpect(model().attribute("score", score))
            .andExpect(view().name("result"));

        verify(resultService).saveResult(eq(result));
        verify(userService).loadUserByUsername(eq(username));
        verify(quizService).findById(eq(quizId));
        verify(mailSender).sendResult(eq(result));
    }
}
