package tests;

import com.vk.api.sdk.actions.Likes;
import com.vk.api.sdk.client.ClientResponse;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.likes.Type;
import com.vk.api.sdk.objects.likes.responses.GetListResponse;
import org.apache.log4j.Logger;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class GetListLikesTest {
    private VkApiClient vk;
    private Likes likes;
    private TransportClient transportClient;
    private UserActor actor;
    private HashMap<String, String> headers;
    private ClientResponse positiveClientResponse, negativeClientResponse;

    final Logger logger = Logger.getLogger(GetListLikesTest.class);

    @BeforeMethod
    public void setup() {
        logger.info("Setup method start");
        //Создаем мок TransportClient: имитируем HTTP-запросы
        transportClient = Mockito.mock(TransportClient.class);

        //Создаем экземпляр VkApiClient с моком TransportClient
        vk = new VkApiClient(transportClient);

        //Создаем экземпляр Likes для работы с методами likes API
        likes = new Likes(vk);

        //Создаем мок актора
        actor = Mockito.mock(UserActor.class);

        //Создаем мок ответа от API в виде JSON (из описания результата с сайта)
        String positiveJsonResponse = "{\"count\": 2, \"items\": [2435, 3578]}";
        String negativeJsonResponse = "{\"error\": {\"error_code\": 15, \"error_msg\": \"Access denied\"}}";

        //Заполняем map хэдером
        headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        //Создаем объекты ответов
        positiveClientResponse = new ClientResponse(200, positiveJsonResponse, headers);
        negativeClientResponse = new ClientResponse(403, negativeJsonResponse, headers);
    }

    @Test
    public void testGetList() throws Exception {
        logger.info("Positive test start");
        //Настраиваем TransportClient для возврата подготовленного ответа
        when(transportClient.post(anyString(), anyString(), (org.apache.http.Header[]) any())).thenReturn(positiveClientResponse);

        try {
            //Выполняем метод likes.getList и проверяем результаты
            GetListResponse listResponse = likes.getList(actor, Type.POST)
                    .ownerId(12345L)
                    .execute();

            //Проверяем, что количество элементов совпадают с ожидаемым
            assertEquals(listResponse.getCount(), Integer.valueOf(2));

            //Получаем  идентификаторов из ответа
            List<Long> actualItems = listResponse.getItems();

            //Преобразуем значения типа long в тип int, так как в документации написано, что возвращается integer
            List<Integer> intValues = actualItems.stream()
                    .map(Long::intValue)
                    .collect(Collectors.toList());

            //Проверяем, что элементы совпадают с ожидаемыми
            assertEquals(intValues, Arrays.asList(2435, 3578));

            //Проверяем, что метод post() был вызван
            verify(transportClient).post(anyString(), anyString(), (org.apache.http.Header[]) any());
            logger.info("Test passed");
        }catch(AssertionError e){
            logger.error("Test failed");
            logger.error(e.getMessage());
            throw e;
        }

    }

    @Test
    public void testNegativeGetList() throws Exception{
        logger.info("Negative test start");
        //Делаем мок для метода post() TransportClient - при отправке определенных параметров возвращаем наш ответ
        when(transportClient.post(anyString(), anyString(), (org.apache.http.Header[]) any())).thenReturn(negativeClientResponse);

        // Проверяем, что при выполнении метода likes.getList с ошибочными данными выбрасывается нужная ошибка
        try {
            vk.likes().getList(actor, Type.POST).execute();
            logger.error("Test failed. Expected error were not occurred");
            fail("Expected ClientException to be thrown");
        } catch (ClientException e) {
            // Проверяем содержимое ошибки
            assertEquals(e.getMessage(), "Internal API server error. Wrong status code: 403. Content: "+negativeClientResponse.getContent());
            logger.info("Test passed");
        }

    }
}

