create table file_blob (
    object_key varchar(200) primary key,
    content bytea not null,
    created_at timestamptz not null
);

alter table beneficiary_photo add column storage_reference varchar(220);
insert into file_blob (object_key, content, created_at)
select 'profile-photo/' || beneficiary_id::text, image, updated_at
from beneficiary_photo;
update beneficiary_photo
set storage_reference = 'postgres:profile-photo/' || beneficiary_id::text;
alter table beneficiary_photo alter column storage_reference set not null;
alter table beneficiary_photo drop column image;

alter table appointment_attachment add column storage_reference varchar(220);
insert into file_blob (object_key, content, created_at)
select 'appointment-order/' || appointment_id::text, content, uploaded_at
from appointment_attachment;
update appointment_attachment
set storage_reference = 'postgres:appointment-order/' || appointment_id::text;
alter table appointment_attachment alter column storage_reference set not null;
alter table appointment_attachment drop column content;

alter table reimbursement_document add column storage_reference varchar(220);
insert into file_blob (object_key, content, created_at)
select 'reimbursement-document/' || id::text, content, uploaded_at
from reimbursement_document;
update reimbursement_document
set storage_reference = 'postgres:reimbursement-document/' || id::text;
alter table reimbursement_document alter column storage_reference set not null;
alter table reimbursement_document drop column content;

alter table preview_document add column storage_reference varchar(220);
insert into file_blob (object_key, content, created_at)
select 'reimbursement-preview/' || id::text, content, uploaded_at
from preview_document;
update preview_document
set storage_reference = 'postgres:reimbursement-preview/' || id::text;
alter table preview_document alter column storage_reference set not null;
alter table preview_document drop column content;
