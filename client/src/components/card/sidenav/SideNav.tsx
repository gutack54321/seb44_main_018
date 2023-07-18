import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useMatch, useNavigate } from 'react-router-dom';
import { useReadLocalStorage } from 'usehooks-ts';
import { patchFeedLike } from '../../../api/mutationfn';
import { SERVER_URL } from '../../../api/url';
import { ReactComponent as Comment } from '../../../assets/button/comment.svg';
import { ReactComponent as Delete } from '../../../assets/button/delete.svg';
import { ReactComponent as Edit } from '../../../assets/button/edit.svg';
import { ReactComponent as Like } from '../../../assets/button/like.svg';
import { ReactComponent as Share } from '../../../assets/button/share.svg';
import { BooleanStr } from '../../../types/propType';
import { Container, Wrap, Text } from './SideNav.styled';

interface Prop {
  feedid: number;
  direction: 'row' | 'col';
  likes: number;
  like: BooleanStr;
  guesthandler?: () => void;
  toasthandler?: React.Dispatch<React.SetStateAction<boolean>>;
  deletehandler?: React.Dispatch<React.SetStateAction<boolean>>;
  url: string;
  inperson?: BooleanStr;
}

export default function SideNav({
  feedid,
  direction,
  likes,
  like,
  guesthandler,
  toasthandler,
  deletehandler,
  inperson,
  url,
}: Prop) {
  const accessToken = useReadLocalStorage<string | null>('accessToken');
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const feedPopUp = useMatch('/home/:feedId');
  const [isLike, setIsLike] = useState<BooleanStr>(like);
  const [isLikes, setIsLikes] = useState<number>(likes);

  const likeMutation = useMutation({
    mutationFn: patchFeedLike,
    onSuccess: ({ data }) => {
      queryClient.invalidateQueries({ queryKey: ['feedPopUp'] });
      setIsLike(data.isLike.toString());
      setIsLikes(data.likeCount);
    },
  });

  const handleClickLike = () => {
    if (!accessToken) {
      if (guesthandler) return guesthandler();
    }
    const data = {
      url: `${SERVER_URL}/feeds/like/${feedid}`,
      accessToken,
    };
    likeMutation.mutate(data);
  };

  const handleClickComment = () => {
    if (!accessToken) {
      if (guesthandler) return guesthandler();
    }
    navigate(`/home/${feedid}`);
  };

  const handleClickShare = async () => {
    await navigator.clipboard.writeText(url);
    if (toasthandler) {
      toasthandler(true);
      setTimeout(() => toasthandler(false), 1500);
    }
  };

  return (
    <>
      <Container direction={direction}>
        <Wrap className="pl-2">
          {isLike === 'true' ? (
            <Like
              className="cursor-pointer"
              stroke="black"
              fill="#69B783"
              onClick={handleClickLike}
            />
          ) : (
            <Like
              className="cursor-pointer "
              stroke="black"
              onClick={handleClickLike}
            />
          )}

          <Text>{isLikes}</Text>
        </Wrap>

        {!feedPopUp && (
          <Wrap onClick={handleClickComment}>
            <Comment className="cursor-pointer ml-2" stroke="black" />
          </Wrap>
        )}

        <Wrap onClick={handleClickShare}>
          <Share className="cursor-pointer ml-2" />
        </Wrap>

        {inperson === 'true' && (
          <>
            <Wrap
              onClick={() => navigate(`/feed-posting/${feedid}`)}
              className="cursor-pointer ml-2">
              <Edit />
            </Wrap>
            <Wrap
              className="cursor-pointer ml-2"
              onClick={() => {
                if (deletehandler) return deletehandler(true);
              }}>
              <Delete />
            </Wrap>
          </>
        )}
      </Container>
    </>
  );
}
