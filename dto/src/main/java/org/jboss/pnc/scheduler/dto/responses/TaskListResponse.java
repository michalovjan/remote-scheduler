package org.jboss.pnc.scheduler.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jboss.pnc.scheduler.dto.TaskDTO;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskListResponse {

    public List<TaskDTO> taskDTOS;
}
