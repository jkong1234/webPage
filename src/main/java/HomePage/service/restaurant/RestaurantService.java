package HomePage.service.restaurant;

import HomePage.config.naver.NaverClient;
import HomePage.domain.model.dto.RestaurantDto;
import HomePage.domain.model.dto.SearchImageReq;
import HomePage.domain.model.dto.SearchImageRes;
import HomePage.domain.model.dto.SearchLocalReq;
import HomePage.domain.model.entity.Page;
import HomePage.domain.model.entity.Restaurant;
import HomePage.mapper.RestaurantMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RestaurantService {
    @Value("${communityBoard.page-size}")
    private int pageSize;

    @Autowired
    private RestaurantMapper restaurantMapper;
    @Autowired
    private NaverClient naverClient;
    @Autowired
    private ObjectMapper objectMapper;
    public Page<RestaurantDto> getRestaurantsPage(int pageNumber){
        if (pageNumber <= 0) {
            throw new IllegalArgumentException("Page number must be greater than 0");
        }

        int totalRestaurants = restaurantMapper.count();

        int totalRestaurantPages = (int) Math.ceil((double) totalRestaurants / pageSize);
        System.out.println("totalPage" + totalRestaurantPages);
        if (pageNumber > totalRestaurantPages) {
            pageNumber = totalRestaurantPages; // or throw an exception if you prefer
        }

        int offset = (pageNumber - 1) * pageSize;
        List<Restaurant> restaurants = restaurantMapper.findRestaurantPage(offset, pageSize);

        // 컨트롤러 계층에 전달하기 위해 entity -> dto 변환
        List<RestaurantDto> restaurantDTOs = restaurants.stream()
                .map(restaurant -> {
                    RestaurantDto restaurantDto = objectMapper.convertValue(restaurant, RestaurantDto.class);
                    return restaurantDto;
                })
                .collect(Collectors.toList());

        return new Page<RestaurantDto>(restaurantDTOs, pageNumber, totalRestaurantPages, pageSize);
    }



    public int createRestaurants(String query) {
        List<String> regions = Arrays.asList("서울", "경기", "인천", "강원", "충청", "대전", "세종", "전라", "광주", "경상", "대구", "부산", "제주");


        int totalSaved = 0;

        for (String region : regions) {
            // 지역 검색
            var searchLocalReq = new SearchLocalReq();
            searchLocalReq.setQuery(query);
            // 지역 검색 응답
            var searchLocalRes = naverClient.searchLocal(searchLocalReq);

            // 검색 결과가 있으면
            if (searchLocalRes.getTotal() > 0){
                var localSearchItem = searchLocalRes.getItems().stream().findFirst().get();
                var localSearchImageQuery = localSearchItem.getTitle().replaceAll("<[^>]*>", "");
                // 이미지 검색
                var localSearchImageReq = new SearchImageReq();
                localSearchImageReq.setQuery(localSearchImageQuery);

                // 이미지 검색 응답
                SearchImageRes searchImageRes = naverClient.searchImage(localSearchImageReq);

                // 이미지 검색이 있다면?
                if(searchImageRes.getTotal() > 0) {
                    var imageItem = searchImageRes.getItems().stream().findFirst().get();
                    // 결과를 리턴
                    // 서비스 계층에서 -> 레퍼지토리 계층 혹은 매퍼 계층으로 이동하므로 Dto를 만들어서 entity로 변환해야함.
                    RestaurantDto restaurantDto = new RestaurantDto();
                    restaurantDto.setTitle(localSearchItem.getTitle());
                    restaurantDto.setCategory(localSearchItem.getCategory());
                    restaurantDto.setAddress(localSearchItem.getAddress());
                    restaurantDto.setRoadAddress(localSearchItem.getRoadAddress());
                    restaurantDto.setHomePageLink(localSearchItem.getLink());
                    restaurantDto.setImageUrl(imageItem.getLink());


                    // dto -> entity 변환
                    Restaurant convertedRestaurant = objectMapper.convertValue(restaurantDto, Restaurant.class);

                    // 데이터 베이스에 삽입
                    restaurantMapper.insert(convertedRestaurant);
                }
                totalSaved++;
            }
        }
        return totalSaved;
    }
//    public List<Map<String, Restaurant>> createRestaurantImage(List<Map<String, String>> restaurants) {
//        List<Map<String, Restaurant>> result = new ArrayList<>();
//        for (Map<String, String> restaurantInfo : restaurants) {
//            String title = restaurantInfo.get("title");
//            String address = restaurantInfo.get("address");
//            String json = naverClient.searchImage(title, address);
//            System.out.println(json);
//            try {
//                JsonNode rootNode = objectMapper.readTree(json);
//                JsonNode items = rootNode.path("items");
//                for (JsonNode item : items) {
//                    String itemAddress = item.path("address").asText();
//                    if (address.equals(itemAddress)) {
//                        Restaurant foundRestaurant = restaurantMapper.selectByTitleAndAddress(title, address);
//                        System.out.println(foundRestaurant);
//                        if (foundRestaurant != null) {
//                            // 이미지 URL이 없는 경우에만 업데이트
//                            if (foundRestaurant.getImageUrl() == null) {
//                                String imageUrl = item.path("thumbnail").asText();
//                                foundRestaurant.setImageUrl(imageUrl);
//                                restaurantMapper.update(foundRestaurant);
//                            }
//
//                            // 데이터베이스에서 최신 정보를 다시 조회
//                            Restaurant updatedRestaurant = restaurantMapper.selectById(foundRestaurant.getId());
//
//                            Map<String, Restaurant> restaurantMap = new HashMap<>();
//                            restaurantMap.put("restaurant", updatedRestaurant);
//                            result.add(restaurantMap);
//                        } else {
//                            System.out.println("Restaurant not found for title: " + title + " and address: " + address);
//                        }
//                        break; // 일치하는 주소를 찾았으므로 루프 종료
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        return result;
//    }

    private Restaurant createRestaurantFromJsonNode(JsonNode item) {
        Restaurant restaurant = new Restaurant();
        restaurant.setTitle(removeHtmlTags(item.path("title").asText()));
        restaurant.setLink(item.path("link").asText());
        restaurant.setCategory(item.path("category").asText());
        restaurant.setDescription(item.path("description").asText());
        restaurant.setTelephone(item.path("telephone").asText());
        restaurant.setAddress(item.path("address").asText());
        restaurant.setRoadAddress(item.path("roadAddress").asText());
        restaurant.setMapx(item.path("mapx").asText());
        restaurant.setMapy(item.path("mapy").asText());
        return restaurant;
   }
    private String removeHtmlTags(String input) {
       return Pattern.compile("<[^>]*>").matcher(input).replaceAll("");
    }

}
