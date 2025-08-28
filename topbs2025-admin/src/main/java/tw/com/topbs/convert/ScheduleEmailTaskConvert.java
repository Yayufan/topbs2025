package tw.com.topbs.convert;

import org.mapstruct.Mapper;

import tw.com.topbs.pojo.DTO.addEntityDTO.AddScheduleEmailTaskDTO;
import tw.com.topbs.pojo.entity.ScheduleEmailTask;

@Mapper(componentModel = "spring")
public interface ScheduleEmailTaskConvert {

	ScheduleEmailTask addDTOToEntity(AddScheduleEmailTaskDTO addScheduleEmailTaskDTO);

	ScheduleEmailTask copyEntity(ScheduleEmailTask scheduleEmailTask);
	
}
