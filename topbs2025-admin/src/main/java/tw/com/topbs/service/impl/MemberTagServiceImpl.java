package tw.com.topbs.service.impl;


import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import tw.com.topbs.mapper.MemberTagMapper;
import tw.com.topbs.pojo.entity.MemberTag;
import tw.com.topbs.service.MemberTagService;

/**
 * <p>
 * member表 和 tag表的關聯表 服务实现类
 * </p>
 *
 * @author Joey
 * @since 2025-01-23
 */
@Service
public class MemberTagServiceImpl extends ServiceImpl<MemberTagMapper, MemberTag> implements MemberTagService {

}
