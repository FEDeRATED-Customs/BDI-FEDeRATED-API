import {DataGrid, GridColDef} from "@mui/x-data-grid";
import {DateField, List, useDataGrid,} from "@refinedev/mui";
import React from "react";

export const SendEventList = () => {

  const {dataGridProps} = useDataGrid({
    syncWithLocation: true,
  });

  const columns = React.useMemo<GridColDef[]>(
    () => {
      return [
        {
          field: "messageId",
          headerName: "messageID",
          type: "string",
          minWidth: 150,
        },
        {
          field: "date",
          headerName: "Date",
          type: "string",
          minWidth: 150,
          renderCell: (params: any) => <DateField format="YYYY-MM-DD HH:mm" value={params.value}/>
        },
        {
          field: "messageType",
          headerName: "Message Type",
          type: "string",
          minWidth: 100,
        },
        {
          field: "destination",
          headerName: "Destinations",
          type: "string",
          minWidth: 300,
          renderCell: (params: any) =>  {
            if (params.value === '') {
                return params.row.distributionType;
            }
            return params.value;
          }
        },
        {
          field: "status",
          headerName: "Status",
          type: "string",
          minWidth: 80,
        },
        {
          field: "message",
          flex: 1,
          headerName: "Message",
          type: "string",
          renderCell: (params: any) => atob(params.value)
        }
      ];
    },
    []
  );

  return (
    <List>
      <DataGrid {...dataGridProps} columns={columns} getRowId={(row: any) =>  row.messageId} autoHeight/>
    </List>
  );

};

