package HomePage.controller.article;

import HomePage.domain.model.CommunityBoard;
import HomePage.domain.model.CommunityComment;
import HomePage.domain.model.Page;
import HomePage.service.CommunityBoardService;
import HomePage.service.CommunityCommentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.List;

@Controller
@RequestMapping("/articles")
public class ArticleController {
    private final CommunityBoardService boardService;
    private final CommunityCommentService commentService;

    public ArticleController(CommunityBoardService boardService, CommunityCommentService commentService) {
        this.boardService = boardService;
        this.commentService = commentService;
    }

    @GetMapping("/{id}")
    public String viewArticle(@PathVariable Long id,
                              @RequestParam(required = false) String topic,
                              @RequestParam(defaultValue = "1") int page,
                              Model model) {

        // 게시글 상세 정보 조회 로직
        CommunityBoard article = boardService.getBoardById(id);
        if (article == null){
            return "error/404";
        }

        List<CommunityComment> comments = commentService.getCommentByBoardId(id);// 게시글 id를 통해서 해당 게시글의 댓글들을 불러온다.
        int commentCnt = commentService.getCommentCntById(id); // 게시글 id를 통해서 해당 게시글의 댓글 수를 불러온다.

        Page<CommunityBoard> boardPage = boardService.getBoardPage(page); // 현재 article의 매핑되어 있는 page를 통해 boarPage를 불러온다.
        int totalPages = boardPage.getTotalPages();
        int currentPage = boardPage.getCurrentPage();
        int visiblePages = 5;
        int start = Math.max(1, currentPage - (visiblePages / 2));
        int end = Math.min(start + visiblePages - 1, totalPages);

        if (end - start + 1 < visiblePages) {
            start = Math.max(1, end - visiblePages + 1);
        }

        model.addAttribute("comments", comments);
        model.addAttribute("article", article);
        model.addAttribute("commentCnt", commentCnt);
        model.addAttribute("topic", topic);
        model.addAttribute("page", page);
        model.addAttribute("boardPage", boardPage);
        model.addAttribute("start", start);
        model.addAttribute("end", end);

        Timestamp timestamp = boardPage.getContent().stream().findAny().get().getRegisterDate();
        System.out.println(timestamp);
        return "/articles/articleViewDetail";
    }
}