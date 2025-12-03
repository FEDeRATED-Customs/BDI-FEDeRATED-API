import { Stack, Typography } from "@mui/material";
import { useOne, useShow } from "@refinedev/core";
import {
  DateField,
  MarkdownField,
  NumberField,
  Show,
  TextFieldComponent as TextField,
} from "@refinedev/mui";

export const EventShow = () => {
  const { queryResult } = useShow({});

  const { data, isLoading } = queryResult;

  const record = data?.data;

  return (
    <Show isLoading={isLoading}>
      <Stack gap={1}>

        <Typography variant="body1" fontWeight="bold">
          {"MessageId"}
        </Typography>
        <TextField value={record?.messageId ?? ""} />

        <Typography variant="body1" fontWeight="bold">
          {"Date"}
        </Typography>
        <TextField value={record?.date} />

        <Typography variant="body1" fontWeight="bold">
          {"Type"}
        </Typography>
        <TextField value={record?.data} />

        <Typography variant="body1" fontWeight="bold">
          {"Status"}
        </Typography>
        <TextField value={record?.status} />

        <Typography variant="body1" fontWeight="bold">
          {"Message"}
        </Typography>
        <TextField value={atob(record?.message)} />

      </Stack>
    </Show>
  );
};
