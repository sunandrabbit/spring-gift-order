package gift.service;

import gift.domain.Order;
import gift.entity.MemberEntity;
import gift.entity.OptionEntity;
import gift.entity.OrderEntity;
import gift.entity.WishListEntity;
import gift.error.NotFoundException;
import gift.repository.MemberRepository;
import gift.repository.OptionRepository;
import gift.repository.OrderRepository;
import gift.repository.WishListRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final OptionRepository optionRepository;
    private final WishListRepository wishListRepository;

    public OrderService(
        OrderRepository orderRepository,
        MemberRepository memberRepository,
        OptionRepository optionRepository,
        WishListRepository wishListRepository) {
        this.orderRepository = orderRepository;
        this.memberRepository = memberRepository;
        this.optionRepository = optionRepository;
        this.wishListRepository = wishListRepository;
    }

    @Transactional
    public Order addOrder(Long memberId, Order order) {
        MemberEntity memberEntity = memberRepository.findById(memberId)
            .orElseThrow(() -> new NotFoundException("Not found Member"));
        OptionEntity optionEntity = optionRepository.findById(order.getOptionId())
            .orElseThrow(() -> new NotFoundException("Not found Option"));

        //주문 저장
        OrderEntity orderEntity = orderRepository.save(
            new OrderEntity(optionEntity, order.getQuantity(), order.getMessage(), LocalDateTime.now()
                ));
        //옵션엔티티에서 주문 수량 만큼 수량 차감 후 저장.
        optionEntity.subtractQuantity(order.getQuantity());
        optionRepository.save(optionEntity);

        //멤버 엔티티의 위시리스트 리스트에서 해당 위시리스트 엔티티 제거,
        memberEntity.removeWishListHasProductEntity(optionEntity.getProductEntity());
        //실제 Repo에서도 제거
        List<WishListEntity> wishListEntities = wishListRepository.findByMemberEntity(memberEntity);
        wishListEntities.stream()
            .filter(wishListEntity -> wishListEntity.equalByProductEntity(optionEntity.getProductEntity()))
            .forEach(wishListEntity -> wishListRepository.deleteById(wishListEntity.getId()));

        return OrderEntity.toDto(orderEntity);
    }
}