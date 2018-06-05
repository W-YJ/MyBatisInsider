package mapper;

import domain.OrderCustom;
import domain.Orders;

import java.util.List;

public interface OrdersMapperCustom {
    //一对一查询，查询订单关联查询用户
    List<OrderCustom> findOrderUserList() throws Exception;

    List<Orders> findOrderUserListResultMap() throws Exception;

    List<Orders> findOrderAndOrderDetails() throws Exception;

    //一对一查询，延迟加载
    List<Orders> findOrderUserListLazyLoading() throws Exception;
}
