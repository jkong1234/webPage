package HomePage.service;

import HomePage.domain.model.Board;
import HomePage.domain.model.Page;

import java.util.List;

public interface BoardService <T extends Board>{
    Page<T> getBoardPage(int pageNumber);
    Page<T> getTopViewedBoardPage(int pageNumber);
    Page<T> getTopCommentCntBoardPage(int pageNumber);
    T getBoardById(Long id);
    void saveBoard(T board);
    void updateBoard(T board);
    void deleteBoard(Long id);
    List<T> searchBoardsByTitle(String title);
    List<T> searchBoardsByWriter(String writer);
    List<T> getAllBoards();

    void incrementViews(Long id);

}
