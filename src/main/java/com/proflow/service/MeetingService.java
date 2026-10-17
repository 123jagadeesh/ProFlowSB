package com.proflow.service;

import com.proflow.dto.auth.UserSummaryDto;
import com.proflow.dto.zoom.MeetingResponse;
import com.proflow.dto.zoom.ScheduleMeetingRequest;
import com.proflow.entity.Company;
import com.proflow.entity.Meeting;
import com.proflow.entity.User;
import com.proflow.exception.BadRequestException;
import com.proflow.exception.ResourceNotFoundException;
import com.proflow.repository.CompanyRepository;
import com.proflow.repository.MeetingRepository;
import com.proflow.repository.UserRepository;
import com.proflow.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final ZoomService zoomService;
    private final EmailService emailService;

    @Transactional
    public MeetingResponse scheduleMeeting(ScheduleMeetingRequest request, UserPrincipal currentUser) {
        if (currentUser.getCompanyId() == null) {
            throw new BadRequestException("Creator must belong to a company to schedule meetings.");
        }

        Company company = companyRepository.findById(currentUser.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        User creator = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Creator user not found"));

        List<User> invitees = userRepository.findAllById(request.getInviteeIds());

        // Validate that all invitees belong to the same company
        boolean allSameCompany = invitees.stream().allMatch(user ->
                user.getCompany() != null && user.getCompany().getId().equals(currentUser.getCompanyId())
        );

        if (!allSameCompany) {
            throw new BadRequestException("Invitees must be from the same company.");
        }

        ZoomService.ZoomMeetingDetails zoomDetails = zoomService.createZoomMeeting(
                request.getTopic(),
                request.getStartTime(),
                request.getDuration()
        );

        Set<User> inviteeSet = new HashSet<>(invitees);

        Meeting meeting = Meeting.builder()
                .topic(request.getTopic())
                .startTime(request.getStartTime())
                .duration(request.getDuration())
                .joinUrl(zoomDetails.joinUrl())
                .startUrl(zoomDetails.startUrl())
                .creator(creator)
                .invitees(inviteeSet)
                .company(company)
                .build();

        meeting = meetingRepository.save(meeting);

        // Send email to invitees
        String formattedTime = DateTimeFormatter.ISO_INSTANT.format(request.getStartTime());
        for (User invitee : invitees) {
            emailService.sendZoomInviteEmail(
                    invitee.getEmail(),
                    meeting.getTopic(),
                    formattedTime,
                    meeting.getJoinUrl(),
                    creator.getName(),
                    company.getName()
            );
        }

        return mapToResponse(meeting);
    }

    private MeetingResponse mapToResponse(Meeting meeting) {
        UserSummaryDto creatorDto = UserSummaryDto.builder()
                .id(meeting.getCreator().getId())
                .name(meeting.getCreator().getName())
                .email(meeting.getCreator().getEmail())
                .role(meeting.getCreator().getRole())
                .build();

        List<UserSummaryDto> inviteeDtos = meeting.getInvitees().stream()
                .map(inv -> UserSummaryDto.builder()
                        .id(inv.getId())
                        .name(inv.getName())
                        .email(inv.getEmail())
                        .role(inv.getRole())
                        .build())
                .collect(Collectors.toList());

        return MeetingResponse.builder()
                .id(meeting.getId())
                .topic(meeting.getTopic())
                .startTime(meeting.getStartTime())
                .duration(meeting.getDuration())
                .joinUrl(meeting.getJoinUrl())
                .startUrl(meeting.getStartUrl())
                .creator(creatorDto)
                .invitees(inviteeDtos)
                .company(meeting.getCompany().getId())
                .createdAt(meeting.getCreatedAt())
                .build();
    }
}
