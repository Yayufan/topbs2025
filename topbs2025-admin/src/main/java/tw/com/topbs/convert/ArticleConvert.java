package tw.com.topbs.convert;

import org.mapstruct.Mapper;

import tw.com.topbs.pojo.DTO.addEntityDTO.AddArticleDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutArticleDTO;
import tw.com.topbs.pojo.entity.Article;

@Mapper(componentModel = "spring")
public interface ArticleConvert {

	Article addDTOToEntity(AddArticleDTO insertArticleDTO);

	Article putDTOToEntity(PutArticleDTO updateArticleDTO);
	
	Article copyEntity(Article article);
	
}
